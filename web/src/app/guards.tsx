import type { ReactNode } from 'react';
import { Navigate, useLocation, useSearchParams } from 'react-router';
import { canUseWeb, homeOf, useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { ForbiddenPage } from './pages';
import { ActivityIndicator } from '@/shared/ui';

/** Signed-in pages: wait for the session to be restored, send anonymous users to /login with ?next=. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { session } = useSession();
  const location = useLocation();
  if (session.status === 'loading') {
    return <ActivityIndicator label={vi.pages.loading} className="is-page" />;
  }
  if (session.status === 'anonymous') {
    const next = `${location.pathname}${location.search}`;
    return <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />;
  }
  return <>{children}</>;
}

/**
 * Dispatcher pages (P-46): OWNER and DISPATCHER. Anyone else signed in (an ADMIN, a DRIVER) gets 403 at the address they
 * opened (P-75); the bare "/" still takes everyone home. Not a security check: the API refuses what they may not do.
 */
export function RequireDispatcher({ children }: { children: ReactNode }) {
  const { session } = useSession();
  const { pathname } = useLocation();
  if (session.status !== 'authenticated') return null;
  if (canUseWeb(session.user)) return <>{children}</>;
  if (pathname === '/') return <Navigate to={homeOf(session.user)} replace />;
  return <ForbiddenPage />;
}

/** The ADMIN area (W11): only an ADMIN; anyone else gets 403 (P-75). Not a security check. */
export function RequireAdmin({ children }: { children: ReactNode }) {
  const { session } = useSession();
  if (session.status !== 'authenticated') return null;
  if (session.user.role !== 'ADMIN') return <ForbiddenPage />;
  return <>{children}</>;
}

/** Pages only some roles may open (OWNER-only: "Nhật ký cửa hàng"): anyone else gets 403 (P-75). Not a security check. */
export function RequireRole({ roles, children }: { roles: string[]; children: ReactNode }) {
  const { session } = useSession();
  if (session.status !== 'authenticated') return null;
  if (!roles.includes(session.user.role)) return <ForbiddenPage />;
  return <>{children}</>;
}

/** "Dùng app tài xế": only for a DRIVER signed in on the web; anyone else gets 403 (P-75). */
export function RequireDriver({ children }: { children: ReactNode }) {
  const { session } = useSession();
  if (session.status !== 'authenticated') return null;
  if (canUseWeb(session.user) || session.user.role === 'ADMIN') {
    return <ForbiddenPage />;
  }
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
