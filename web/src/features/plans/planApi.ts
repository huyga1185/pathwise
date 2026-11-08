import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import { useApi } from '@/data/ApiContext';
import { unwrap } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import type { Schemas } from '@/data/types';

export type PlanSummary = Schemas['PlanSummaryResponse'];
export type PlanDetail = Schemas['PlanDetailResponse'];
export type PlanStop = Schemas['PlanStopResponse'];
export type PlanStatus = PlanDetail['status'];
export type PlanDraft = Schemas['PlanDraftRequest'];
export type StopPatch = Schemas['PatchStopRequest'];
export type AssignableMember = Schemas['MemberResponse'];
export type ActiveVehicle = Schemas['VehicleResponse'];

/** While the server works on a plan (geocode, optimize) it is read again this often (UX_BRIEF §5.1: 2–3 s). */
export const POLL_INTERVAL_MS = 2000;
/** A plan being delivered (PUBLISHED, IN_PROGRESS) is read again this often while the tab is visible (§5). */
export const TRACK_INTERVAL_MS = 30_000;
export type Job = Schemas['JobResponse'];
export const PAGE_SIZE = 20;

export interface PlanFilter {
  date?: string;
  status?: PlanStatus;
  page: number;
}

/** `GET /plans`, newest plan date first (API_SPEC §7). */
export function usePlans(filter: PlanFilter) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.plans(filter),
    queryFn: () =>
      unwrap(
        api.GET('/api/v1/plans', {
          params: { query: { date: filter.date, status: filter.status, page: filter.page, size: PAGE_SIZE } },
        }),
      ),
    placeholderData: (previous) => previous,
  });
}

/** How often a plan is read again (ARCHITECTURE §5): never once it has ended. */
export function planRefetchInterval(plan: PlanDetail | undefined): number | false {
  if (!plan) return false;
  if (plan.processingStatus !== 'IDLE') return POLL_INTERVAL_MS;
  if (plan.status === 'PUBLISHED' || plan.status === 'IN_PROGRESS') return TRACK_INTERVAL_MS;
  return false;
}

/**
 * `GET /plans/{id}`, read again every 2 s while `processingStatus` is not `IDLE`, every 30 s while delivered (only
 * while the tab is visible: TanStack Query pauses intervals in the background), not at all once ended. The `version`
 * of the last read is the one to send.
 */
export function usePlan(id: number, enabled = true) {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.plan(id),
    queryFn: () => unwrap(api.GET('/api/v1/plans/{id}', { params: { path: { id } } })),
    enabled,
    refetchInterval: (query) => planRefetchInterval(query.state.data),
    refetchIntervalInBackground: false,
  });
}

/**
 * `GET /jobs/{id}` until it ends (API_SPEC §6): the plan goes back to IDLE without saying how its job ended, so a
 * failed optimize is only known from the job. `job` is the one the POST returned (already ended in inline mode).
 */
export function useJob(job: Job | null) {
  const api = useApi();
  const id = job?.id ?? 0;
  return useQuery({
    queryKey: ['job', id],
    queryFn: () => unwrap(api.GET('/api/v1/jobs/{id}', { params: { path: { id } } })),
    enabled: job !== null && !isFinished(job),
    initialData: job ?? undefined,
    refetchInterval: (query) => (query.state.data && !isFinished(query.state.data) ? POLL_INTERVAL_MS : false),
  });
}

export function isFinished(job: Job): boolean {
  return job.status === 'SUCCEEDED' || job.status === 'FAILED';
}

/** People who can be given a route (`assignable=true`: DRIVER or `canDrive`, ACTIVE). */
export function useAssignableMembers() {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.assignableMembers,
    queryFn: () => unwrap(api.GET('/api/v1/store/members', { params: { query: { assignable: true } } })),
  });
}

/** Vehicles in use (`GET /vehicles?active=true`), shared with the vehicles screen's cache. */
export function useActiveVehicles() {
  const api = useApi();
  return useQuery({
    queryKey: queryKeys.vehicles({ active: true }),
    queryFn: () => unwrap(api.GET('/api/v1/vehicles', { params: { query: { active: true } } })),
  });
}

/**
 * Changes to a plan (API_SPEC §7). Each throws an ApiError; a changed plan replaces the cached one (its new `version`
 * included) and the lists are refreshed.
 */
export function usePlanApi() {
  const api = useApi();
  const queryClient = useQueryClient();
  return useMemo(() => {
    const store = (plan: PlanDetail) => {
      queryClient.setQueryData(queryKeys.plan(plan.id), plan);
      void queryClient.invalidateQueries({ queryKey: queryKeys.plansAll });
      return plan;
    };
    const reload = (id: number) => queryClient.invalidateQueries({ queryKey: queryKeys.plan(id) });
    return {
      // POST takes no version; the generated type requires one (F-51).
      create: async (body: Omit<PlanDraft, 'version'>) =>
        store(await unwrap(api.POST('/api/v1/plans', { body: body as PlanDraft }))),
      replace: async (id: number, body: PlanDraft) =>
        store(await unwrap(api.PUT('/api/v1/plans/{id}', { params: { path: { id } }, body }))),
      patchStop: async (id: number, stopId: number, body: StopPatch) =>
        store(await unwrap(api.PATCH('/api/v1/plans/{id}/stops/{stopId}', { params: { path: { id, stopId } }, body }))),
      /** Geocodes again every stop FAILED or PENDING; the plan is read again to follow the job. */
      geocode: async (id: number, version: number) => {
        await unwrap(api.POST('/api/v1/plans/{id}/geocode', { params: { path: { id } }, body: { version } }));
        await reload(id);
      },
      /** Starts the optimize job and returns it (it may have ended already, inline mode). */
      optimize: async (id: number, version: number) => {
        const job = await unwrap(
          api.POST('/api/v1/plans/{id}/optimize', { params: { path: { id } }, body: { version } }),
        );
        await reload(id);
        return job;
      },
      publish: async (id: number, version: number) =>
        store(await unwrap(api.POST('/api/v1/plans/{id}/publish', { params: { path: { id } }, body: { version } }))),
      cancel: async (id: number, version: number) =>
        store(await unwrap(api.POST('/api/v1/plans/{id}/cancel', { params: { path: { id } }, body: { version } }))),
      reload,
    };
  }, [api, queryClient]);
}

/** Every stop of a plan: in its routes (by sequence) then the ones without a route. */
export function allStops(plan: PlanDetail): PlanStop[] {
  return [...plan.routes.flatMap((route) => route.stops), ...plan.unassignedStops];
}
