import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { ApiError } from '@/data/apiError';
import { routeColour } from '@/platform/map';
import { messageFor } from '@/shared/errors';
import { vi } from '@/shared/i18n/vi';
import { Alert, ErrorBanner, Sheet } from '@/shared/ui';
import { violationText } from './manualEdit';
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
 * "Giao việc" (SCREENS §1.1; G5.13, G5-Q7): a sheet — who gets which route, the stops left without a driver, the
 * constraints still broken, what changes after (a list is too long for an alert: HIG Alerts, "Keep alert messages
 * short"). "Huỷ" leads, the verb trails (HIG Sheets). SHIFT_OVERLAP turns the same sheet into the clashes, each with a
 * link to the other plan, and "Sửa phân công"; the plan is unchanged.
 */
export function PublishSheet({
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
  const p = vi.publish;

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
      <Sheet
        open={open}
        title={p.overlapSheetTitle}
        doneLabel={p.editAssignments}
        cancelLabel={p.close}
        onSubmit={(event) => {
          event.preventDefault();
          navigate(`/plans/${plan.id}/edit`);
        }}
        onClose={close}
      >
        <p className="pw-sheet-lead">{p.overlapQuestion}</p>
        {clashes.map((clash) => {
          const route = plan.routes[clash.index];
          const shift = route ? `${route.shiftStart}–${route.shiftEnd}` : '';
          return (
            <div key={`${clash.index}-${clash.kind}-${clash.conflictPlanId}`} className="pw-overlap">
              <p>
                {clash.kind === 'driver'
                  ? p.overlapDriver(route ? driverName(route) : '', shift, dayMonth(plan.planDate))
                  : p.overlapVehicle(route?.vehicle.name ?? '', shift, dayMonth(plan.planDate))}
              </p>
              <p className="pw-overlap-link">
                <span>{p.conflictWith(clash.conflictPlanId)}</span>
                <Link to={`/plans/${clash.conflictPlanId}`} onClick={close}>
                  {p.openConflict(clash.conflictPlanId)}
                </Link>
              </p>
            </div>
          );
        })}
        <p className="pw-muted">{p.overlapLead}</p>
      </Sheet>
    );
  }

  return (
    <Sheet
      open={open}
      title={p.sheetTitle}
      doneLabel={plan.violations.length > 0 ? p.confirmAnyway : p.confirm}
      busy={busy}
      onSubmit={(event) => {
        event.preventDefault();
        void publish();
      }}
      onClose={close}
    >
      <p className="pw-sheet-lead">{p.question(routes.length)}</p>
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <ul className="pw-publish-facts">
        {plan.routes.map((route, index) =>
          route.stops.length > 0 ? (
            <li key={route.id}>
              <span className={`pw-dot pw-swatch-r${routeColour(index)}`} aria-hidden="true" />
              <span>
                <strong>{driverName(route)}</strong>
                {` · ${p.routeLine(route.vehicle.name, route.stops.length, spanText(route, plan.timezone))}`}
              </span>
            </li>
          ) : null,
        )}
        {left.length > 0 ? (
          <li>
            <span className="pw-dot pw-dot-signal" aria-hidden="true" />
            <span>{p.unassigned(left.map(stopTitle).join(', '))}</span>
          </li>
        ) : null}
      </ul>
      {plan.violations.length > 0 ? (
        <div className="pw-publish-violations">
          <p>
            <strong>{vi.manual.violationsTitle(plan.violations.length)}</strong>
          </p>
          <ul>
            {plan.violations.map((violation, index) => (
              <li key={`${violation.code}-${violation.routeId}-${violation.stopId ?? ''}-${index}`}>
                {violationText(plan, violation)}
              </li>
            ))}
          </ul>
          <p>{p.violationsLead}</p>
        </div>
      ) : null}
      <p className="pw-muted">{p.lead}</p>
    </Sheet>
  );
}

/**
 * Cancelling the plan (DRAFT or PUBLISHED, API_SPEC §7; G5.14): an alert — the consequence as its message, "Huỷ"
 * (HIG Alerts: "Always use the title "Cancel" for a button that cancels an alert's action") and the destructive
 * "Huỷ kế hoạch". A failure is said in the message, the alert stays.
 */
export function CancelAlert({
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

  const consequence = `${plan.status === 'DRAFT' ? vi.cancelPlan.draft : vi.cancelPlan.published} ${vi.cancelPlan.after}`;
  return (
    <Alert
      open={open}
      title={vi.cancelPlan.title(plan.name)}
      message={failure ? `${messageFor(failure)} ${consequence}` : consequence}
      actionLabel={vi.cancelPlan.confirm}
      destructive
      busy={busy}
      onAction={() => void cancel()}
      onCancel={() => {
        setFailure(null);
        onClose();
      }}
    />
  );
}
