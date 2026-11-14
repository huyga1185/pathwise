import { keepPreviousData, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApi } from '@/data/ApiContext';
import { unwrap, unwrapEmpty } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import type { Schemas } from '@/data/types';

export type AdminStoreSummary = Schemas['AdminStoreSummaryResponse'];
export type AdminStoreDetail = Schemas['AdminStoreDetailResponse'];
export type AdminUser = Schemas['AdminGetUserResponse'];
export type AuditEvent = Schemas['AuditEventResponse'];
export type BusinessMetrics = Schemas['BusinessMetricsResponse'];
export type TechnicalMetrics = Schemas['TechnicalMetricsResponse'];

export interface StoreFilter {
  q?: string;
  status?: 'ACTIVE' | 'READ_ONLY';
  page: number;
  sort: 'name,asc' | 'name,desc' | 'id,desc';
}

export interface UserFilter {
  storeId?: number;
  role?: AdminUser['role'];
  status?: AdminUser['status'];
  page: number;
}

export interface AuditFilter {
  storeId?: number;
  actorId?: number;
  action?: AuditEvent['action'];
  from?: string;
  to?: string;
  page: number;
}

const PAGE_SIZE = 20;

/** Stores of the platform (`GET /admin/stores`, T55). */
export function useAdminStores(filter: StoreFilter) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.adminStores(filter),
    queryFn: () =>
      unwrap(
        api.GET('/api/v1/admin/stores', {
          params: {
            query: {
              q: filter.q || undefined,
              status: filter.status,
              page: filter.page,
              size: PAGE_SIZE,
              sort: [filter.sort],
            },
          },
        }),
      ),
    placeholderData: keepPreviousData,
  });
}

/**
 * One store (`GET /admin/stores/{id}`, T55). Each call is recorded as STORE_VIEWED (T58) that the OWNER sees, so
 * it is never fetched again by itself: no refetch on focus or interval, only when the ADMIN opens or reloads it.
 */
export function useAdminStore(id: number) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.adminStore(id),
    queryFn: () => unwrap(api.GET('/api/v1/admin/stores/{id}', { params: { path: { id } } })),
    staleTime: Infinity,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });
}

/** Users of the platform (`GET /admin/users`, T55). */
export function useAdminUsers(filter: UserFilter) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.adminUsers(filter),
    queryFn: () =>
      unwrap(
        api.GET('/api/v1/admin/users', {
          params: {
            query: {
              storeId: filter.storeId,
              role: filter.role,
              status: filter.status,
              page: filter.page,
              size: PAGE_SIZE,
              sort: ['id,asc'],
            },
          },
        }),
      ),
    placeholderData: keepPreviousData,
  });
}

/** The audit log of the platform (`GET /admin/audit`, T58), newest first. */
export function useAdminAudit(filter: AuditFilter, size = PAGE_SIZE) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.adminAudit({ ...filter, size }),
    queryFn: () =>
      unwrap(
        api.GET('/api/v1/admin/audit', {
          params: {
            query: {
              storeId: filter.storeId,
              actorId: filter.actorId,
              action: filter.action,
              from: filter.from || undefined,
              to: filter.to || undefined,
              page: filter.page,
              size,
              sort: ['occurredAt,desc'],
            },
          },
        }),
      ),
    placeholderData: keepPreviousData,
  });
}

/** Business numbers by day (`GET /admin/metrics/business`, T60), cached a minute by the server. */
export function useBusinessMetrics(from: string, to: string) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.adminBusiness({ from, to }),
    queryFn: () => unwrap(api.GET('/api/v1/admin/metrics/business', { params: { query: { from, to } } })),
    placeholderData: keepPreviousData,
  });
}

/** Technical numbers of the running instance (`GET /admin/metrics/technical`, T60), read again every minute. */
export function useTechnicalMetrics() {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.adminTechnical,
    queryFn: () => unwrap(api.GET('/api/v1/admin/metrics/technical')),
    refetchInterval: 60_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * ADMIN actions (T56, T57). Each throws an ApiError; each reloads what it changed. A changed store detail is updated
 * from the response, not fetched again (that would record another STORE_VIEWED).
 */
export function useAdminApi() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMemo(() => {
    const setStoreState = (state: Schemas['AdminStoreStateResponse']) => {
      queryClient.setQueryData<AdminStoreDetail>(queryKeys.adminStore(state.id), (detail) =>
        detail ? { ...detail, status: state.status, lockReason: state.lockReason, lockedAt: state.lockedAt } : detail,
      );
      void queryClient.invalidateQueries({ queryKey: ['admin', 'stores'] });
    };
    const reloadUsers = () => queryClient.invalidateQueries({ queryKey: queryKeys.adminUsersAll });
    return {
      lockStore: async (id: number, reason: string) =>
        setStoreState(
          await unwrap(api.POST('/api/v1/admin/stores/{id}/lock', { params: { path: { id } }, body: { reason } })),
        ),
      unlockStore: async (id: number) =>
        setStoreState(await unwrap(api.POST('/api/v1/admin/stores/{id}/unlock', { params: { path: { id } } }))),
      /** The store detail is fetched again on purpose: the roles changed. */
      transferOwner: async (id: number, userId: number) => {
        const result = await unwrap(
          api.POST('/api/v1/admin/stores/{id}/owner', { params: { path: { id } }, body: { userId } }),
        );
        await queryClient.invalidateQueries({ queryKey: queryKeys.adminStore(id) });
        void queryClient.invalidateQueries({ queryKey: ['admin', 'stores'] });
        void reloadUsers();
        return result;
      },
      disableUser: async (id: number) => {
        await unwrap(api.POST('/api/v1/admin/users/{id}/disable', { params: { path: { id } } }));
        await reloadUsers();
      },
      enableUser: async (id: number) => {
        await unwrap(api.POST('/api/v1/admin/users/{id}/enable', { params: { path: { id } } }));
        await reloadUsers();
      },
      revokeSessions: (id: number) =>
        unwrapEmpty(api.POST('/api/v1/admin/users/{id}/sessions/revoke', { params: { path: { id } } })),
      sendVerificationEmail: (id: number) =>
        unwrapEmpty(api.POST('/api/v1/admin/users/{id}/verification-email', { params: { path: { id } } })),
      sendPasswordResetLink: (id: number) =>
        unwrapEmpty(api.POST('/api/v1/admin/users/{id}/password-reset-email', { params: { path: { id } } })),
    };
  }, [api, queryClient]);
}
