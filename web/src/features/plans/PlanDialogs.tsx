import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { ApiError } from '@/data/apiError';
import { routeColour } from '@/platform/map';
import { messageFor } from '@/shared/errors';
import { vi } from '@/shared/i18n/vi';
import { Dialog, ErrorBanner } from '@/shared/ui';
import { usePlanApi, type PlanDetail } from './planApi';
import { driverName, spanText, stopTitle } from './planShared';

/** Routes that have stops: the ones a driver receives (P-34). */
export function routesWithStops(plan: PlanDetail) {
  return plan.routes.filter((route) => route.stops.length > 0);
}

/** "01/12" from "2026-12-01". */
function dayMonth(localDate: string): string {
  const [, month, day] = localDate.split('-');
  return `${day}/${month}`;
}

interface Overlap {
  index: number;
  kind: 'driver' | 'vehicle';
  conflictPlanId: number;
}

/** `details` of SHIFT_OVERLAP (API_SPEC §7): `assignments[i].driverId|vehicleId`, i in `PlanDetail.routes` order. */
function overlaps(error: ApiError): Overlap[] {
  return error.details.flatMap((detail) => {
    const match = /^assignments\[(\d+)\]\.(driverId|vehicleId)$/.exec(detail.field ?? '');
    const conflict = Number((detail as { conflictPlanId?: unknown }).conflictPlanId);
    return match && Number.isFinite(conflict)
      ? [
          {
            index: Number(match[1]),
            kind: match[2] === 'driverId' ? ('driver' as const) : ('vehicle' as const),
            conflictPlanId: conflict,
          },
        ]
      : [];
  });
}

/**
 * Prototype "Xác nhận giao việc" (SCREENS §1.1): who gets which route, the stops left without a driver, what changes
 * after. SHIFT_OVERLAP opens a second dialog naming each clash with a link to the other plan; the plan is unchanged.
 */
export function PublishDialog({
  plan,
  open,
  onClose,
  onPublished,
}: {
  plan: PlanDetail;
  open: boolean;
  onClose: () => void;
  onPublished: (drivers: number) => void;
}) {
  const planApi = usePlanApi();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const [clashes, setClashes] = useState<Overlap[] | null>(null);
  const routes = routesWithStops(plan);
  const left = plan.unassignedStops;

  const publish = async () => {
    setBusy(true);
    setFailure(null);
    try {
      await planApi.publish(plan.id, plan.version);
      onPublished(routes.length);
    } catch (error) {
      if (error instanceof ApiError && error.code === 'SHIFT_OVERLAP') setClashes(overlaps(error));
      else setFailure(error);
      if (error instanceof ApiError && error.code === 'PLAN_MODIFIED') await planApi.reload(plan.id);
    } finally {
      setBusy(false);
    }
  };

  const close = () => {
    setFailure(null);
    setClashes(null);
    onClose();
  };

  if (clashes) {
    return (
      <Dialog
        open={open}
        title={vi.publish.overlapTitle}
        confirmLabel={vi.publish.editAssignments}
        cancelLabel={vi.publish.close}
        onConfirm={() => navigate(`/plans/${plan.id}/edit`)}
        onClose={close}
      >
        {clashes.map((clash) => {
          const route = plan.routes[clash.index];
          const shift = route ? `${route.shiftStart}–${route.shiftEnd}` : '';
          return (
            <div key={`${clash.index}-${clash.kind}-${clash.conflictPlanId}`} className="pw-overlap">
              <p>
                {clash.kind === 'driver'
                  ? vi.publish.overlapDriver(route ? driverName(route) : '', shift, dayMonth(plan.planDate))
                  : vi.publish.overlapVehicle(route?.vehicle.name ?? '', shift, dayMonth(plan.planDate))}
              </p>
              <p className="pw-overlap-link">
                <span>{vi.publish.conflictWith(clash.conflictPlanId)}</span>
                <Link to={`/plans/${clash.conflictPlanId}`} onClick={close}>
                  {vi.publish.openConflict(clash.conflictPlanId)}
                </Link>
              </p>
            </div>
          );
        })}
        <p>{vi.publish.overlapLead}</p>
      </Dialog>
    );
  }

  return (
    <Dialog
      open={open}
      title={vi.publish.title(routes.length)}
      confirmLabel={vi.publish.confirm}
      cancelLabel={vi.publish.back}
      busy={busy}
      onConfirm={() => void publish()}
      onClose={close}
    >
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <ul className="pw-publish-facts">
        {plan.routes.map((route, index) =>
          route.stops.length > 0 ? (
            <li key={route.id}>
              <span className={`pw-dot pw-swatch-r${routeColour(index)}`} aria-hidden="true" />
              <span>
                <strong>{driverName(route)}</strong>
                {` · ${vi.publish.routeLine(route.vehicle.name, route.stops.length, spanText(route, plan.timezone))}`}
              </span>
            </li>
          ) : null,
        )}
        {left.length > 0 ? (
          <li>
            <span className="pw-dot pw-dot-signal" aria-hidden="true" />
            <span>{vi.publish.unassigned(left.map(stopTitle).join(', '))}</span>
          </li>
        ) : null}
      </ul>
      <p>{vi.publish.lead}</p>
    </Dialog>
  );
}

/** Cancelling (DRAFT or PUBLISHED, API_SPEC §7): `danger`, the consequence in the body, the error inside. */
export function CancelDialog({
  plan,
  open,
  onClose,
  onCancelled,
}: {
  plan: PlanDetail;
  open: boolean;
  onClose: () => void;
  onCancelled: () => void;
}) {
  const planApi = usePlanApi();
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);

  const cancel = async () => {
    setBusy(true);
    setFailure(null);
    try {
      await planApi.cancel(plan.id, plan.version);
      onCancelled();
    } catch (error) {
      setFailure(error);
      if (error instanceof ApiError && (error.code === 'PLAN_MODIFIED' || error.code === 'PLAN_NOT_CANCELLABLE')) {
        await planApi.reload(plan.id);
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog
      open={open}
      tone="danger"
      title={vi.cancelPlan.title(plan.name)}
      confirmLabel={vi.cancelPlan.confirm}
      cancelLabel={vi.publish.back}
      busy={busy}
      onConfirm={() => void cancel()}
      onClose={() => {
        setFailure(null);
        onClose();
      }}
    >
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <p>{plan.status === 'DRAFT' ? vi.cancelPlan.draft : vi.cancelPlan.published}</p>
      <p>{vi.cancelPlan.after}</p>
    </Dialog>
  );
}
