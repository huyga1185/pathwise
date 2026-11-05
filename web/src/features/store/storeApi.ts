import { useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApi } from '@/data/ApiContext';
import { unwrap } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import type { Schemas } from '@/data/types';

export type Store = Schemas['StoreResponse'];

/** `PUT /store` (OWNER, API_SPEC §4): the whole store; `depot: null` means no depot. The header shows the new name. */
export function useStoreApi() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMemo(
    () => ({
      update: async (body: Schemas['UpdateStoreRequest']) => {
        const store = await unwrap(api.PUT('/api/v1/store', { body }));
        queryClient.setQueryData(queryKeys.store, store);
        return store;
      },
    }),
    [api, queryClient],
  );
}
