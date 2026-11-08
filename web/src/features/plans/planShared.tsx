import type { ReactNode } from 'react';
import { formatCapacity } from '@/features/fleet';
import { formatCoordinates } from '@/features/store';
import { routeColour, type MapRoute, type MapStop, type MarkerTone } from '@/platform/map';
import { formatTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import type { PlanDetail, PlanStop } from './planApi';

export type PlanRoute = PlanDetail['routes'][number];

/** "ĐH-1009 · Chị Lan", or the label alone. */
export function stopTitle(stop: PlanStop): string {
  return stop.recipientName ? `${stop.label} · ${stop.recipientName}` : stop.label;
}

/** Address, or the coordinates given for a stop without one. */
export function stopPlace(stop: PlanStop): string {
  if (stop.address) return stop.address;
  return stop.lat !== null && stop.lon !== null
    ? vi.result.coordsOnly(formatCoordinates({ lat: stop.lat, lon: stop.lon }))
    : '';
}

export function windowText(stop: PlanStop): string {
  return stop.timeWindow ? `${stop.timeWindow.start}–${stop.timeWindow.end}` : '';
}

/** "10:50–13:23" in the plan's timezone; empty for a route with no stop. */
export function spanText(route: PlanRoute, timeZone: string): string {
  return route.plannedStart && route.plannedEnd
    ? `${formatTime(route.plannedStart, timeZone)}–${formatTime(route.plannedEnd, timeZone)}`
    : '';
}

/** "Xe 1 · 30 thùng": the vehicle and its capacity snapshot of the assignment. */
export function vehicleText(route: PlanRoute): string {
  return `${route.vehicle.name} · ${formatCapacity(route.vehicle.capacity, route.vehicle.capacityUnit)}`;
}

/** "22/30 thùng": the load the solver planned against the capacity, in the vehicle's unit (D-36). */
export function loadText(route: PlanRoute): string {
  if (route.load === null) return '';
  const unit = vi.units.short[route.vehicle.capacityUnit] ?? route.vehicle.capacityUnit;
  return `${route.load.toLocaleString('vi-VN')}/${route.vehicle.capacity.toLocaleString('vi-VN')} ${unit}`;
}

export function driverName(route: PlanRoute): string {
  return route.driver.fullName ?? vi.members.noName;
}

/** Marker colour of a stop by its result (DESIGN_SYSTEM MapMarker); none while pending: the route's colour. */
export function resultTone(stop: PlanStop): MarkerTone | undefined {
  if (stop.status === 'DELIVERED') return 'success';
  if (stop.status === 'FAILED') return 'danger';
  if (stop.status === 'SKIPPED') return 'signal';
  return undefined;
}

/** The plan as RouteMap data: routes in API order (colour n), stops by sequence, stops without a route apart. */
export function mapData(plan: PlanDetail, withResults: boolean): { routes: MapRoute[]; unassigned: MapStop[] } {
  const located = (stop: PlanStop) => stop.lat !== null && stop.lon !== null;
  return {
    routes: plan.routes.map((route) => ({
      key: route.id,
      label: driverName(route),
      stops: route.stops.filter(located).map((stop) => ({
        id: stop.id,
        lat: stop.lat as number,
        lon: stop.lon as number,
        sequence: stop.sequence,
        tone: withResults ? resultTone(stop) : undefined,
        label: `${driverName(route)} · ${stop.sequence ?? ''} · ${stopTitle(stop)}`,
      })),
    })),
    unassigned: plan.unassignedStops.filter(located).map((stop) => ({
      id: stop.id,
      lat: stop.lat as number,
      lon: stop.lon as number,
      tone: stop.dropReason === 'INFEASIBLE' ? 'signal' : 'muted',
      label: `${vi.result.noRoute} · ${stopTitle(stop)}`,
    })),
  };
}

export const MAP_LABELS = {
  zoomIn: vi.map.zoomIn,
  zoomOut: vi.map.zoomOut,
  depot: vi.map.depot,
  expand: vi.map.expand,
  collapse: vi.map.collapse,
  legend: vi.map.legend,
};

/** Coloured disc with a van (prototype route header): the route's map colour, so the list matches the map. */
export function RouteBadge({ index }: { index: number }) {
  return (
    <svg viewBox="0 0 28 28" width="28" height="28" aria-hidden="true" focusable="false" className="pw-route-badge">
      <circle className={`pw-fill-r${routeColour(index)}`} cx="14" cy="14" r="14" />
      <g fill="none" stroke="#ffffff" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
        <path d="M6.5 9.5h9v8h-9z" />
        <path d="M15.5 12h3.2l2.8 2.9v2.6h-6" />
        <circle cx="9.5" cy="18.3" r="1.5" />
        <circle cx="18" cy="18.3" r="1.5" />
      </g>
    </svg>
  );
}

const STAT_ART: Record<string, ReactNode> = {
  stops: (
    <>
      <path d="M12 21.5s-7-6.2-7-11.7a7 7 0 0 1 14 0c0 5.5-7 11.7-7 11.7z" />
      <circle cx="12" cy="9.8" r="2.6" />
    </>
  ),
  distance: (
    <>
      <circle cx="5" cy="18.5" r="2.2" />
      <path d="M7.2 18.5H13a3.2 3.2 0 0 0 0-6.4H9.5a3.2 3.2 0 0 1 0-6.4h3" strokeDasharray="2.2 2" />
      <path d="M18.5 10.5s-3.5-3-3.5-5.6a3.5 3.5 0 0 1 7 0c0 2.6-3.5 5.6-3.5 5.6z" />
    </>
  ),
  duration: (
    <>
      <circle cx="12" cy="12.5" r="8.5" />
      <path d="M12 7.5v5l3.2 2" />
      <path d="M9.5 2.5h5" />
    </>
  ),
  drivers: (
    <>
      <path d="M2.5 6.5h11v10h-11z" />
      <path d="M13.5 9.5h4l3.5 3.6v3.4h-7.5" />
      <circle cx="6.5" cy="17.5" r="1.8" />
      <circle cx="17" cy="17.5" r="1.8" />
    </>
  ),
};

/** Overview card (prototype): small caps label, value, a faint picture in the corner. */
export function StatCard({ art, label, value }: { art?: keyof typeof STAT_ART; label: ReactNode; value: ReactNode }) {
  return (
    <div className="pw-stat">
      {art ? (
        <svg className="pw-stat-art" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          {STAT_ART[art]}
        </svg>
      ) : null}
      <span className="pw-stat-label">{label}</span>
      <span className="pw-stat-value">{value}</span>
    </div>
  );
}
