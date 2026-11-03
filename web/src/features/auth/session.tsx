import { useQueryClient } from '@tanstack/react-query';
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { useApiContext } from '@/data/ApiContext';
import { unwrap, unwrapEmpty } from '@/data/http';
import type { UserRole } from '@/data/types';

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

function toUser(
  raw:
    { id?: number; email?: string; fullName?: string; role?: string; storeId?: number; canDrive?: boolean } | undefined,
): SessionUser {
  if (!raw || raw.id === undefined || !raw.email || !raw.role) throw new Error('Incomplete user in the API response');
  return {
    id: raw.id,
    email: raw.email,
    fullName: raw.fullName,
    role: raw.role as UserRole,
    storeId: raw.storeId,
    canDrive: raw.canDrive === true,
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

  const logIn = useCallback(
    async (email: string, password: string) => {
      const tokens = await unwrap(api.client.POST('/api/v1/auth/log-in', { body: { email, password } }));
      tokenStore.set(tokens.accessToken ?? null);
      const user = toUser(tokens.user);
      setSession({ status: 'authenticated', user });
      return user;
    },
    [api, tokenStore],
  );

  const logOut = useCallback(async () => {
    try {
      await unwrapEmpty(api.client.POST('/api/v1/auth/log-out'));
    } catch {
      // The session ends on this device either way.
    }
    tokenStore.set(null);
    queryClient.clear();
    setSession({ status: 'anonymous', expired: false });
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

/** Who may use the dispatcher web (P-46): OWNER and DISPATCHER. A DRIVER is sent to the app. */
export function canUseWeb(user: SessionUser): boolean {
  return user.role === 'OWNER' || user.role === 'DISPATCHER';
}
