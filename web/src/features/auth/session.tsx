import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { useApiContext } from '@/data/ApiContext';
import { unwrap, unwrapEmpty } from '@/data/http';
import type { AuthUser, UserRole } from '@/data/types';
import { accountScreenShown, appShown, changeScene } from './sceneTransition';

export interface SessionUser {
  id: number;
  email: string;
  fullName?: string;
  role: UserRole;
  storeId?: number;
  canDrive: boolean;
}

export type Session =
  { status: 'loading' } | { status: 'anonymous'; expired: boolean } | { status: 'authenticated'; user: SessionUser };

interface SessionValue {
  session: Session;
  /** @throws ApiError for a failed sign in */
  logIn(email: string, password: string): Promise<SessionUser>;
  logOut(): Promise<void>;
  /** Changes the signed-in user after their profile was saved (`PATCH /users/me`), so the header shows it. */
  updateUser(changes: Partial<Pick<SessionUser, 'fullName'>>): void;
}

const SessionContext = createContext<SessionValue | null>(null);

/** The user of `LoginResponse.user` or `GET /users/me` (API_SPEC §2, §3); a missing name or store is `undefined`. */
function toUser(
  raw: Pick<AuthUser, 'id' | 'email' | 'role' | 'canDrive'> & {
    fullName?: string | null;
    storeId?: number | null;
  },
): SessionUser {
  return {
    id: raw.id,
    email: raw.email,
    fullName: raw.fullName ?? undefined,
    role: raw.role,
    storeId: raw.storeId ?? undefined,
    canDrive: raw.canDrive,
  };
}

/**
 * The web session (P-46, docs/client/ARCHITECTURE.md §4): restored from the refresh cookie on load, the access token
 * kept in memory only, cleared with the query cache on sign out or when a refresh fails.
 */
export function SessionProvider({ children }: { children: ReactNode }) {
  const { api, tokenStore, sessionEvents } = useApiContext();
  const queryClient = useQueryClient();
  const [session, setSession] = useState<Session>({ status: 'loading' });

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        if (!(await api.refresh())) throw new Error('no session');
        const me = await unwrap(api.client.GET('/api/v1/users/me'));
        if (!cancelled) setSession({ status: 'authenticated', user: toUser(me) });
      } catch {
        tokenStore.set(null);
        if (!cancelled) setSession({ status: 'anonymous', expired: false });
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [api, tokenStore]);

  useEffect(() => {
    const onExpired = () => {
      tokenStore.set(null);
      queryClient.clear();
      setSession({ status: 'anonymous', expired: true });
    };
    sessionEvents.addEventListener('expired', onExpired);
    return () => sessionEvents.removeEventListener('expired', onExpired);
  }, [sessionEvents, tokenStore, queryClient]);

  // A log-out still on its way (it runs in the background, below): signing in again waits for it, or its answer, which
  // clears the refresh cookie, could clear the new session's cookie.
  const pendingLogOut = useRef<Promise<void> | null>(null);

  const logIn = useCallback(
    async (email: string, password: string) => {
      await pendingLogOut.current;
      const tokens = await unwrap(api.client.POST('/api/v1/auth/log-in', { body: { email, password } }));
      // `user` is left out only by /refresh (TokenResponse); log-in always has it.
      if (!tokens.user) throw new Error('Log-in response without user');
      tokenStore.set(tokens.accessToken);
      const user = toUser(tokens.user);
      // The login screen gives way to the app through a transition, not a cut; the login button keeps turning until the
      // screen has faded out.
      await changeScene(() => setSession({ status: 'authenticated', user }), appShown);
      return user;
    },
    [api, tokenStore],
  );

  const logOut = useCallback(async () => {
    // The session ends on this device at once and the screen changes right away; the server is told in the background
    // (POST /log-out is public and works from the cookie, so it needs no access token). Waiting for it first left the
    // app on screen for seconds after "Đăng xuất".
    const request = unwrapEmpty(api.client.POST('/api/v1/auth/log-out')).catch(() => {
      // The session ends on this device either way.
    });
    pendingLogOut.current = request;
    tokenStore.set(null);
    void changeScene(() => {
      queryClient.clear();
      setSession({ status: 'anonymous', expired: false });
    }, accountScreenShown);
    await request;
    if (pendingLogOut.current === request) pendingLogOut.current = null;
  }, [api, tokenStore, queryClient]);

  const updateUser = useCallback((changes: Partial<Pick<SessionUser, 'fullName'>>) => {
    setSession((current) =>
      current.status === 'authenticated' ? { status: 'authenticated', user: { ...current.user, ...changes } } : current,
    );
  }, []);

  const value = useMemo(() => ({ session, logIn, logOut, updateUser }), [session, logIn, logOut, updateUser]);
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionValue {
  const value = useContext(SessionContext);
  if (!value) throw new Error('useSession must be used inside SessionProvider');
  return value;
}

/** Where a signed-in user starts (W11): the ADMIN area, the dispatcher pages, or "Dùng app tài xế". */
export function homeOf(user: SessionUser): string {
  if (user.role === 'ADMIN') return '/admin';
  return canUseWeb(user) ? '/plans' : '/driver';
}

/** Who may use the dispatcher web (P-46): OWNER and DISPATCHER. A DRIVER is sent to the app. */
export function canUseWeb(user: SessionUser): boolean {
  return user.role === 'OWNER' || user.role === 'DISPATCHER';
}
