import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApi } from '@/data/ApiContext';
import { unwrap } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import type { Schemas } from '@/data/types';

export type Vehicle = Schemas['VehicleResponse'];
export type CapacityUnit = Vehicle['capacityUnit'];

/** Vehicles of the store by name (`GET /vehicles`); `active: true` leaves out the ones turned off. */
export function useVehicles(filter: { active?: boolean }) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.vehicles(filter),
    queryFn: () => unwrap(api.GET('/api/v1/vehicles', { params: { query: filter } })),
  });
}

/** Add and change a vehicle (API_SPEC §5). Each throws an ApiError; the lists are reloaded after a change. */
export function useVehicleApi() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMemo(() => {
    const reload = () => queryClient.invalidateQueries({ queryKey: queryKeys.vehiclesAll });
    return {
      create: async (body: Schemas['CreateVehicleRequest']) => {
        const vehicle = await unwrap(api.POST('/api/v1/vehicles', { body }));
        await reload();
        return vehicle;
      },
      update: async (id: number, body: Schemas['UpdateVehicleRequest']) => {
        const vehicle = await unwrap(api.PUT('/api/v1/vehicles/{id}', { params: { path: { id } }, body }));
        await reload();
        return vehicle;
      },
    };
  }, [api, queryClient]);
}
