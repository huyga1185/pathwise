import { useState } from 'react';
import { useNavigate } from 'react-router';
import { useStoreQuery } from '@/data/storeQuery';
import { fieldErrors, messageFor } from '@/shared/errors';
import { todayIn } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Dialog, ErrorBanner, TextField } from '@/shared/ui';
import { allStops, usePlan, usePlanApi, type PlanDetail, type SkippedAssignment } from './planApi';

const t = vi.planCopy;

/** What the new plan's page says once (router state of the navigation after a copy). */
export interface CopiedNotice {
  from: string;
  stops: number;
  all: boolean;
  /** Assignments left out, already as text ("tài xế Hải (không còn nhận tuyến được)"). */
  skipped: string[];
}

export interface CopyDialogProps {
  planId: number;
  planName: string;
  open: boolean;
  onClose: () => void;
}

/**
 * Prototype "Kế hoạch — sao chép" (W12, T62): day, name, and which stops; the new DRAFT opens with a notice of what
 * was not copied. "Only the stops not delivered" shows only when the plan has some and was given to drivers.
 */
export function CopyDialog({ planId, planName, open, onClose }: CopyDialogProps) {
  const navigate = useNavigate();
  const planApi = usePlanApi();
  const store = useStoreQuery();
  const plan = usePlan(planId, open);
  const timeZone = store.data?.timezone ?? 'Asia/Ho_Chi_Minh';
  const [date, setDate] = useState(() => todayIn(timeZone));
  const [name, setName] = useState(() => t.nameOf(planName));
  const [scope, setScope] = useState<'ALL' | 'NOT_DELIVERED'>('ALL');
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);

  const counts = plan.data ? countStops(plan.data) : null;
  const offerNotDelivered = plan.data !== undefined && plan.data.status !== 'DRAFT' && (counts?.notDelivered ?? 0) > 0;
  const fields = fieldErrors(failure, { planDate: t.date, name: t.name });

  const close = () => {
    setFailure(null);
    onClose();
  };
  const copy = async () => {
    setBusy(true);
    setFailure(null);
    try {
      const chosen = offerNotDelivered ? scope : 'ALL';
      const result = await planApi.copy(planId, { planDate: date, name: name.trim() || undefined, stops: chosen });
      const notice: CopiedNotice = {
        from: planName,
        stops: allStops(result.plan).length,
        all: chosen === 'ALL',
        skipped: plan.data ? describeSkipped(plan.data, result.skippedAssignments) : [],
      };
      onClose();
      navigate(`/plans/${result.plan.id}`, { state: { copied: notice } });
    } catch (error) {
      setFailure(error);
    } finally {
      setBusy(false);
    }
  };

  return (
    <Dialog
      open={open}
      title={t.title(planName)}
      confirmLabel={t.confirm}
      cancelLabel={t.back}
      busy={busy}
      confirmDisabled={!date || !plan.data}
      onConfirm={() => void copy()}
      onClose={close}
    >
      <div className="pw-copy-form">
        <TextField
          label={t.date}
          type="date"
          required
          value={date}
          error={fields.planDate}
          onChange={(event) => setDate(event.target.value)}
        />
        <TextField
          label={t.name}
          value={name}
          maxLength={150}
          error={fields.name}
          onChange={(event) => setName(event.target.value)}
        />
        {plan.isError ? <ErrorBanner title={messageFor(plan.error)} /> : null}
        {counts ? (
          <fieldset className="pw-copy-scope">
            <legend className="pw-field-label">{t.scope}</legend>
            <label className="pw-copy-option">
              <input
                type="radio"
                name="copy-scope"
                checked={!offerNotDelivered || scope === 'ALL'}
                onChange={() => setScope('ALL')}
              />
              <span>
                <span className="pw-copy-option-title">{t.all(counts.all)}</span>
                <span className="pw-muted">{t.allHint}</span>
              </span>
            </label>
            {offerNotDelivered ? (
              <label className="pw-copy-option">
                <input
                  type="radio"
                  name="copy-scope"
                  checked={scope === 'NOT_DELIVERED'}
                  onChange={() => setScope('NOT_DELIVERED')}
                />
                <span>
                  <span className="pw-copy-option-title">{t.notDelivered(counts.notDelivered)}</span>
                  <span className="pw-muted">{t.notDeliveredHint(counts.failed, counts.skipped, counts.pending)}</span>
                </span>
              </label>
            ) : null}
          </fieldset>
        ) : plan.isError ? null : (
          <p className="pw-muted">{vi.pages.loading}</p>
        )}
        <p className="pw-muted">{t.hint}</p>
        {failure && !fields.planDate && !fields.name ? <ErrorBanner title={messageFor(failure)} /> : null}
      </div>
    </Dialog>
  );
}

function countStops(plan: PlanDetail) {
  const stops = allStops(plan);
  const failed = stops.filter((stop) => stop.status === 'FAILED').length;
  const skipped = stops.filter((stop) => stop.status === 'SKIPPED').length;
  const pending = stops.filter((stop) => stop.status === 'PENDING').length;
  return { all: stops.length, failed, skipped, pending, notDelivered: failed + skipped + pending };
}

/** Names come from the copied plan: the response only has ids. */
function describeSkipped(plan: PlanDetail, skipped: SkippedAssignment[]): string[] {
  return skipped.map((item) => {
    const route = plan.routes.find((candidate) => candidate.driver.id === item.driverId);
    if (item.reason === 'VEHICLE_INACTIVE') {
      const vehicle = plan.routes.find((candidate) => candidate.vehicle.id === item.vehicleId)?.vehicle.name;
      return t.vehicleInactive(vehicle ?? `#${item.vehicleId}`);
    }
    return t.driverUnavailable(route?.driver.fullName || `#${item.driverId}`);
  });
}
