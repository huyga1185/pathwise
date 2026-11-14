import { useQuery } from '@tanstack/react-query';
import { useApi } from './ApiContext';
import { unwrap } from './http';
import { queryKeys } from './queryKeys';

/** The signed-in user (`GET /users/me`), with the state of their store (T56). One cache entry for every screen. */
export function useMeQuery() {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.me,
    queryFn: () => unwrap(api.GET('/api/v1/users/me')),
    // The lock can start at any time: look again now and then, and when the user comes back to the tab.
    refetchInterval: 60_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Whether the store of the signed-in member is locked by an ADMIN (T56, P-62): its writes get 423 STORE_READ_ONLY,
 * so screens disable them. `reason` is shown in the lock banner. Never true for an ADMIN (no store).
 */
export function useStoreReadOnly(): { readOnly: boolean; reason: string | null } {
  const me = useMeQuery();
  const readOnly = me.data?.storeStatus === 'READ_ONLY';
  return { readOnly, reason: readOnly ? (me.data?.storeLockReason ?? null) : null };
}
