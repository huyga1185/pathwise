import type { Schemas } from '@/data/types';
import { formatDistance } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import type { PlanDetail, PlanStop } from './planApi';
import { driverName, stopTitle } from './planShared';

/**
 * Manual route editing (W10, T28, P-58): the draft the dispatcher drags around, what it means for each route, the
 * request that saves it, and the sentences of the violations the server reports. Pure: the screen only renders.
 */

type PlanRoute = PlanDetail['routes'][number];
export type Violation = Schemas['ViolationResponse'];
export type ReplaceRoutes = Schemas['ReplaceRoutesRequest'];

/** Column id of the stops without a route; the routes' columns are `route-<id>`. */
export const UNASSIGNED = 'unassigned';

/** Stop ids (as strings, the drag-and-drop ids) by column, routes first in the plan's order, then unassigned. */
export type Draft = Record<string, string[]>;

export const routeColumn = (routeId: number) => `route-${routeId}`;

/** The plan as it is: the starting draft. */
export function draftOf(plan: PlanDetail): Draft {
  const draft: Draft = {};
  for (const route of plan.routes) draft[routeColumn(route.id)] = route.stops.map((stop) => String(stop.id));
  draft[UNASSIGNED] = plan.unassignedStops.map((stop) => String(stop.id));
  return draft;
}

export function draftChanged(plan: PlanDetail, draft: Draft): boolean {
  const original = draftOf(plan);
  return Object.keys(original).some((column) => (original[column] ?? []).join() !== (draft[column] ?? []).join());
}

/** Moves a stop to the end of another column (the "Chuyển sang…" menu). */
export function moveStop(draft: Draft, stopId: string, to: string): Draft {
  const next: Draft = {};
  for (const [column, ids] of Object.entries(draft)) next[column] = ids.filter((id) => id !== stopId);
  next[to] = [...(next[to] ?? []), stopId];
  return next;
}

/**
 * The draft still fits the plan: the same routes and exactly the same stops. After the plan changed elsewhere, a draft
 * that no longer fits is dropped.
 */
export function draftFits(plan: PlanDetail, draft: Draft): boolean {
  const original = draftOf(plan);
  if (Object.keys(original).sort().join() !== Object.keys(draft).sort().join()) return false;
  const ids = (board: Draft) => Object.values(board).flat().sort().join();
  return ids(original) === ids(draft);
}

/** Body of `PUT /plans/{id}/routes`: every route in the plan's order, every stop once. */
export function replaceRequest(plan: PlanDetail, draft: Draft): ReplaceRoutes {
  return {
    version: plan.version,
    routes: plan.routes.map((route) => ({
      routeId: route.id,
      stopIds: (draft[routeColumn(route.id)] ?? []).map(Number),
    })),
    unassignedStopIds: (draft[UNASSIGNED] ?? []).map(Number),
  };
}

/**
 * The plan as the draft would make it, for the map and the column headers: stops moved and renumbered, times and
 * distances unknown until saved.
 */
export function draftPlan(plan: PlanDetail, draft: Draft): PlanDetail {
  const stops = new Map<number, PlanStop>();
  for (const stop of [...plan.routes.flatMap((route) => route.stops), ...plan.unassignedStops])
    stops.set(stop.id, stop);
  const pick = (ids: string[] | undefined) =>
    (ids ?? []).map(Number).flatMap((id) => (stops.has(id) ? [stops.get(id) as PlanStop] : []));
  return {
    ...plan,
    routes: plan.routes.map((route) => ({
      ...route,
      stops: pick(draft[routeColumn(route.id)]).map((stop, index) => ({ ...stop, sequence: index + 1 })),
    })),
    unassignedStops: pick(draft[UNASSIGNED]).map((stop) => ({ ...stop, sequence: null })),
  };
}

/** Load of a route's stops in the vehicle's unit (D-36): the number of stops for ORDER, else the sum of that unit. */
export function draftLoad(route: PlanRoute, stops: PlanStop[]): number {
  const unit = route.vehicle.capacityUnit;
  if (unit === 'ORDER') return stops.length;
  return stops.reduce((sum, stop) => sum + ((stop.demands as Record<string, number | undefined>)[unit] ?? 0), 0);
}

const minutes = (seconds: number) => Math.ceil(seconds / 60);

/** "Tài xế B · Xe 2: vượt tải 3 thùng" and the like (UX_BRIEF §6.3). */
export function violationText(plan: PlanDetail, violation: Violation): string {
  const route = plan.routes.find((item) => item.id === violation.routeId);
  const routeName = route ? `${driverName(route)} · ${route.vehicle.name}` : '';
  switch (violation.code) {
    case 'CAPACITY_EXCEEDED': {
      const unit = route ? (vi.units.short[route.vehicle.capacityUnit] ?? route.vehicle.capacityUnit) : '';
      return vi.manual.capacity(routeName, `${violation.excess.toLocaleString('vi-VN')} ${unit}`.trim());
    }
    case 'TIME_WINDOW_MISSED': {
      const stop = route?.stops.find((item) => item.id === violation.stopId);
      return vi.manual.window(stop ? stopTitle(stop) : routeName, minutes(violation.excess));
    }
    case 'SHIFT_EXCEEDED':
      return vi.manual.shift(routeName, minutes(violation.excess));
    case 'MAX_DISTANCE_EXCEEDED':
      return vi.manual.distance(routeName, formatDistance(violation.excess));
  }
}

/** Minutes late at each stop, for the chip on its row. */
export function lateMinutes(plan: PlanDetail): Map<number, number> {
  const late = new Map<number, number>();
  for (const violation of plan.violations) {
    if (violation.code === 'TIME_WINDOW_MISSED' && violation.stopId !== null)
      late.set(violation.stopId, minutes(violation.excess));
  }
  return late;
}

/** The stop to select on the map for a violation: the late stop, else the route's first stop. */
export function violationStop(plan: PlanDetail, violation: Violation): number | null {
  if (violation.stopId !== null) return violation.stopId;
  return plan.routes.find((route) => route.id === violation.routeId)?.stops[0]?.id ?? null;
}
