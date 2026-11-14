import type { CapacityUnit } from '@/features/fleet';
import { fieldMessage } from '@/shared/errors';
import { formatPhone } from '@/shared/format';
import type { ImportedStop, PlanDetail, PlanDraft } from './planApi';

/** Units a stop's demand can be given in (ORDER is every stop counting one, never entered; D-36). */
export const DEMAND_UNITS = ['KG', 'BOX', 'PACKAGE'] as const;
export type DemandUnit = (typeof DEMAND_UNITS)[number];

export const MAX_STOPS = 100;
export const MAX_ASSIGNMENTS = 20;

export interface AssignmentRow {
  driverId: string;
  vehicleId: string;
  shiftStart: string;
  shiftEnd: string;
  /** Kilometres as typed; the API takes metres. */
  maxDistanceKm: string;
}

export interface StopRow {
  /** Stop of the plan being edited (PUT keeps its geocoding result). */
  id: number | null;
  label: string;
  recipientName: string;
  recipientPhone: string;
  address: string;
  timeWindow: { start: string; end: string };
  demands: Record<DemandUnit, string>;
  note: string;
  serviceTimeSeconds: number | null;
  /** Coordinates the user gave before (PROVIDED): sent again while the address is unchanged. */
  providedAt: { lat: number; lon: number; address: string } | null;
}

export interface PlanFormValues {
  name: string;
  planDate: string;
  assignments: AssignmentRow[];
  stops: StopRow[];
}

export const emptyAssignment = (): AssignmentRow => ({
  driverId: '',
  vehicleId: '',
  shiftStart: '08:00',
  shiftEnd: '17:00',
  maxDistanceKm: '',
});

export const emptyStop = (): StopRow => ({
  id: null,
  label: '',
  recipientName: '',
  recipientPhone: '',
  address: '',
  timeWindow: { start: '', end: '' },
  demands: { KG: '', BOX: '', PACKAGE: '' },
  note: '',
  serviceTimeSeconds: null,
  providedAt: null,
});

/** The form of a new plan: today, one empty stop. */
export function newPlanValues(today: string): PlanFormValues {
  return { name: '', planDate: today, assignments: [], stops: [emptyStop()] };
}

/** The form of a draft being edited, from `GET /plans/{id}`. */
export function planToValues(plan: PlanDetail): PlanFormValues {
  const stops = [...plan.routes.flatMap((route) => route.stops), ...plan.unassignedStops].sort((a, b) => a.id - b.id);
  return {
    name: plan.name,
    planDate: plan.planDate,
    assignments: plan.routes.map((route) => ({
      driverId: String(route.driver.id),
      vehicleId: String(route.vehicle.id),
      shiftStart: route.shiftStart,
      shiftEnd: route.shiftEnd,
      maxDistanceKm: route.maxDistanceMeters === null ? '' : String(route.maxDistanceMeters / 1000),
    })),
    stops: stops.map((stop) => ({
      id: stop.id,
      label: stop.label,
      recipientName: stop.recipientName ?? '',
      recipientPhone: stop.recipientPhone ?? '',
      address: stop.address ?? '',
      timeWindow: { start: stop.timeWindow?.start ?? '', end: stop.timeWindow?.end ?? '' },
      demands: {
        KG: stop.demands.KG !== undefined ? String(stop.demands.KG) : '',
        BOX: stop.demands.BOX !== undefined ? String(stop.demands.BOX) : '',
        PACKAGE: stop.demands.PACKAGE !== undefined ? String(stop.demands.PACKAGE) : '',
      },
      note: stop.note ?? '',
      serviceTimeSeconds: stop.serviceTimeSeconds,
      providedAt:
        stop.geocodeStatus === 'PROVIDED' && stop.lat !== null && stop.lon !== null
          ? { lat: stop.lat, lon: stop.lon, address: stop.address ?? '' }
          : null,
    })),
  };
}

const text = (value: string) => value.trim() || undefined;

/** The body of `POST /plans` / `PUT /plans/{id}` (without `version`) from the form. */
export function valuesToRequest(values: PlanFormValues): Omit<PlanDraft, 'version'> {
  return {
    name: values.name.trim(),
    planDate: values.planDate,
    assignments: values.assignments.map((row) => ({
      driverId: Number(row.driverId),
      vehicleId: Number(row.vehicleId),
      shiftStart: row.shiftStart,
      shiftEnd: row.shiftEnd,
      maxDistanceMeters: row.maxDistanceKm.trim() ? Math.round(Number(row.maxDistanceKm) * 1000) : undefined,
    })),
    stops: values.stops.map((row) => {
      const address = text(row.address);
      // Coordinates the user gave stay while the address does not change; otherwise the server geocodes.
      const keep = row.providedAt && (address ?? '') === row.providedAt.address.trim() ? row.providedAt : null;
      const demands = Object.fromEntries(
        DEMAND_UNITS.filter((unit) => row.demands[unit].trim() !== '').map((unit) => [unit, Number(row.demands[unit])]),
      );
      return {
        id: row.id ?? undefined,
        label: row.label.trim(),
        recipientName: text(row.recipientName),
        recipientPhone: text(row.recipientPhone),
        address,
        lat: keep?.lat,
        lon: keep?.lon,
        demands: Object.keys(demands).length > 0 ? demands : undefined,
        serviceTimeSeconds: row.serviceTimeSeconds ?? undefined,
        timeWindow: row.timeWindow.start || row.timeWindow.end ? { ...row.timeWindow } : undefined,
        note: text(row.note),
      };
    }),
  };
}

/** `stops[3].timeWindow.end` (API_SPEC §1) → `stops.3.timeWindow.end` (react-hook-form). */
export function formPath(field: string): string {
  return field.replace(/\[(\d+)\]/g, '.$1');
}

/** Demand columns: units of the vehicles chosen (ORDER needs none), plus units stops already have. */
export function demandUnits(vehicleUnits: CapacityUnit[], stops: StopRow[]): DemandUnit[] {
  const used = new Set<string>(vehicleUnits);
  stops.forEach((stop) => DEMAND_UNITS.forEach((unit) => stop.demands[unit].trim() && used.add(unit)));
  return DEMAND_UNITS.filter((unit) => used.has(unit));
}

export type RowErrors = Record<string, string>;

/**
 * Instant checks that repeat the server's (ARCHITECTURE §2 rule 5): required fields, time window and shift order,
 * numbers. Keys are form paths; the server's own errors replace these.
 */
export function validatePlan(values: PlanFormValues, labels: Record<string, string>): RowErrors {
  const errors: RowErrors = {};
  const label = (key: string) => labels[key] ?? key;
  if (!values.name.trim()) errors.name = fieldMessage('PLAN_NAME_REQUIRED', label('name'));
  if (!values.planDate) errors.planDate = fieldMessage('PLAN_DATE_REQUIRED', label('planDate'));
  values.assignments.forEach((row, i) => {
    const at = `assignments.${i}`;
    if (!row.driverId) errors[`${at}.driverId`] = fieldMessage('DRIVER_REQUIRED', label('driverId'));
    if (!row.vehicleId) errors[`${at}.vehicleId`] = fieldMessage('VEHICLE_REQUIRED', label('vehicleId'));
    if (!row.shiftStart) errors[`${at}.shiftStart`] = fieldMessage('SHIFT_START_REQUIRED', label('shiftStart'));
    if (!row.shiftEnd) errors[`${at}.shiftEnd`] = fieldMessage('SHIFT_END_REQUIRED', label('shiftEnd'));
    else if (row.shiftStart && row.shiftStart >= row.shiftEnd)
      errors[`${at}.shiftEnd`] = fieldMessage('INVALID_SHIFT', '');
    const km = row.maxDistanceKm.trim();
    if (km && !(Number(km) > 0))
      errors[`${at}.maxDistanceKm`] = fieldMessage('INVALID_MAX_DISTANCE', label('maxDistanceKm'));
  });
  values.stops.forEach((row, i) => {
    const at = `stops.${i}`;
    if (!row.label.trim()) errors[`${at}.label`] = fieldMessage('STOP_LABEL_REQUIRED', label('label'));
    if (!row.address.trim() && !row.providedAt) {
      errors[`${at}.address`] = fieldMessage('ADDRESS_OR_COORDINATES_REQUIRED', label('address'));
    }
    const { start, end } = row.timeWindow;
    if (start && !end) errors[`${at}.timeWindow.end`] = fieldMessage('TIME_WINDOW_END_REQUIRED', label('windowTo'));
    else if (end && !start)
      errors[`${at}.timeWindow.start`] = fieldMessage('TIME_WINDOW_START_REQUIRED', label('windowFrom'));
    else if (start && end && start >= end) errors[`${at}.timeWindow.end`] = fieldMessage('INVALID_TIME_WINDOW', '');
    DEMAND_UNITS.forEach((unit) => {
      const value = row.demands[unit].trim();
      if (value && !(Number.isInteger(Number(value)) && Number(value) >= 0)) {
        errors[`${at}.demands.${unit}`] = fieldMessage('INVALID_DEMAND', '');
      }
    });
  });
  return errors;
}

/**
 * A stop read from an import file (T61) as a row of the form. Coordinates from the file are kept like ones the user
 * gave (`providedAt`): sent again while the address does not change.
 */
export function importedToRow(stop: ImportedStop): StopRow {
  const amount = (unit: DemandUnit) => (stop.demands[unit] !== undefined ? String(stop.demands[unit]) : '');
  return {
    ...emptyStop(),
    label: stop.label ?? '',
    recipientName: stop.recipientName ?? '',
    recipientPhone: formatPhone(stop.recipientPhone),
    address: stop.address ?? '',
    timeWindow: { start: stop.timeWindow?.start ?? '', end: stop.timeWindow?.end ?? '' },
    demands: { KG: amount('KG'), BOX: amount('BOX'), PACKAGE: amount('PACKAGE') },
    note: stop.note ?? '',
    serviceTimeSeconds: stop.serviceTimeSeconds ?? null,
    providedAt:
      stop.lat != null && stop.lon != null ? { lat: stop.lat, lon: stop.lon, address: stop.address ?? '' } : null,
  };
}

/** A row nobody has typed in: the first row of a new plan, replaced by imported stops. */
export function isBlankStop(row: StopRow): boolean {
  return (
    row.id === null &&
    !row.label.trim() &&
    !row.recipientName.trim() &&
    !row.recipientPhone.trim() &&
    !row.address.trim() &&
    !row.note.trim() &&
    !row.timeWindow.start &&
    !row.timeWindow.end &&
    DEMAND_UNITS.every((unit) => !row.demands[unit].trim()) &&
    row.providedAt === null
  );
}
