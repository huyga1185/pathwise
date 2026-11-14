import type { ReactNode } from 'react';
import { Navigate, useLocation, useSearchParams } from 'react-router';
import { canUseWeb, homeOf, useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';

/** Signed-in pages: wait for the session to be restored, send anonymous users to /login with ?next=. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { session } = useSession();
  const location = useLocation();
  if (session.status === 'loading') {
    return (
      <p role="status" className="pw-page-loading">
        {vi.pages.loading}
      </p>
    );
  }
  if (session.status === 'anonymous') {
    const next = `${location.pathname}${location.search}`;
    return <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />;
  }
  return <>{children}</>;
}

/** Dispatcher pages (P-46): OWNER and DISPATCHER; a DRIVER sees "Dùng app tài xế". Not a security check. */
export function RequireDispatcher({ children }: { children: ReactNode }) {
  const { session } = useSession();
  if (session.status !== 'authenticated') return null;
  if (!canUseWeb(session.user)) return <Navigate to={homeOf(session.user)} replace />;
  return <>{children}</>;
}

/** The ADMIN area (W11): anyone else goes back to their own home. Not a security check. */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { session } = useSession();
  if (session.status !== 'authenticated') return null;
  if (session.user.role !== 'ADMIN') return <Navigate to={homeOf(session.user)} replace />;
  return <>{children}</>;
}

/** Pages only some roles may open (OWNER-only settings in later tasks). */
export function RequireRole({ roles, children }: { roles: string[]; children: ReactNode }) {
  const { session } = useSession();
  if (session.status !== 'authenticated') return null;
  if (!roles.includes(session.user.role)) return <Navigate to="/forbidden" replace />;
  return <>{children}</>;
}

/** An in-app path from ?next=, never another site (open redirect). */
export function safeNext(next: string | null): string | null {
  return next && next.startsWith('/') && !next.startsWith('//') && !next.startsWith('/\\') ? next : null;
}

/** /login for someone signed in goes to ?next= or their home. */
export function RedirectIfSignedIn({ children }: { children: ReactNode }) {
  const { session } = useSession();
  const [params] = useSearchParams();
  if (session.status === 'authenticated') {
    const next = safeNext(params.get('next'));
    const home = homeOf(session.user);
    // ?next= only within the user's own area: an ADMIN never lands on a dispatcher page, nor the reverse.
    const sameArea = next !== null && next.startsWith('/admin') === (session.user.role === 'ADMIN');
    const destination = canUseWeb(session.user) || session.user.role === 'ADMIN' ? (sameArea ? next : home) : home;
    return <Navigate to={destination} replace />;
  }
  return <>{children}</>;
}
