import { useQuery } from '@tanstack/react-query';
import { useApi } from './ApiContext';
import { unwrap } from './http';
import { queryKeys } from './queryKeys';

/**
 * The store of the signed-in user (`GET /store`): name for the header, timezone for dates. One cache entry.
 * `enabled` is false for an ADMIN, who has no store (the call would get 403).
 */
export function useStoreQuery(enabled = true) {
  const api = useApi();
  return useQuery({ queryKey: queryKeys.store, queryFn: () => unwrap(api.GET('/api/v1/store')), enabled });
}
