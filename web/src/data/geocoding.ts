import { useCallback } from 'react';
import { useApi } from './ApiContext';
import { unwrap } from './http';

/**
 * Address search through the backend (`GET /geocoding/search`, Vietnam only; ARCHITECTURE §7: the client never calls a
 * geocoding provider itself). Throws an ApiError (GEOCODING_BUSY, GEOCODING_PROVIDER_ERROR…).
 */
export function useGeocodingSearch() {
  const api = useApi();
  return useCallback(
    (query: string) => unwrap(api.GET('/api/v1/geocoding/search', { params: { query: { q: query, limit: 5 } } })),
    [api],
  );
}
