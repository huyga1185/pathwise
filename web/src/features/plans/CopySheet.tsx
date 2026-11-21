import { useState, type KeyboardEvent } from 'react';
import { useNavigate } from 'react-router';
import { useStoreQuery } from '@/data/storeQuery';
import { fieldErrors, messageFor } from '@/shared/errors';
import { todayIn } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { ActivityIndicator, CheckIcon, DateField, ErrorBanner, Sheet, TextField } from '@/shared/ui';
import { cx } from '@/shared/ui/cx';
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

export interface CopySheetProps {
  planId: number;
  planName: string;
  open: boolean;
  onClose: () => void;
}

/**
 * "Sao chép" (W12, T62; OWNER_UI_REDESIGN G4.10): a sheet ("Huỷ" / "Sao chép") with the plan copied from, the day,
 * the name and which stops; the new DRAFT opens with a notice of what was not copied. "Only the stops not delivered"
 * shows only when the plan has some and was given to drivers; the choice is a list of options, the chosen one with a
 * checkmark (HIG Lists and tables: "a table that lists options… adding an image — such as a checkmark").
 */
export function CopySheet({ planId, planName, open, onClose }: CopySheetProps) {
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

  const options = counts
    ? [
        { value: 'ALL' as const, title: t.all(counts.all), hint: t.allHint },
        ...(offerNotDelivered
          ? [
              {
                value: 'NOT_DELIVERED' as const,
                title: t.notDelivered(counts.notDelivered),
                hint: t.notDeliveredHint(counts.failed, counts.skipped, counts.pending),
              },
            ]
          : []),
      ]
    : [];
  const chosen = offerNotDelivered ? scope : 'ALL';
  const onOptionKey = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (!['ArrowDown', 'ArrowUp', 'ArrowLeft', 'ArrowRight'].includes(event.key) || options.length < 2) return;
    event.preventDefault();
    const next = chosen === 'ALL' ? 'NOT_DELIVERED' : 'ALL';
    setScope(next);
    (event.currentTarget.parentElement?.querySelector(`[data-value="${next}"]`) as HTMLElement | null)?.focus();
  };

  return (
    <Sheet
      open={open}
      title={t.sheetTitle}
      doneLabel={t.confirm}
      busy={busy}
      doneDisabled={!date || !plan.data}
      onSubmit={(event) => {
        event.preventDefault();
        void copy();
      }}
      onClose={close}
    >
      <div className="pw-copy-form">
        {/* The plan copied from, in full (the short title leaves it out). */}
        <p className="pw-copy-source">
          <span className="pw-muted">{t.from}</span> <strong>{planName}</strong>
        </p>
        <DateField
          label={t.date}
          required
          value={date}
          onChange={setDate}
          today={todayIn(timeZone)}
          error={fields.planDate}
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
          options.length > 1 ? (
            <div className="pw-copy-scope">
              <span id="copy-scope" className="pw-field-label">
                {t.scope}
              </span>
              <div role="radiogroup" aria-labelledby="copy-scope" className="pw-option-list">
                {options.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    role="radio"
                    data-value={option.value}
                    aria-checked={chosen === option.value}
                    tabIndex={chosen === option.value ? 0 : -1}
                    className={cx('pw-option', chosen === option.value && 'is-chosen')}
                    onClick={() => setScope(option.value)}
                    onKeyDown={onOptionKey}
                  >
                    <span className="pw-option-text">
                      <span className="pw-option-title">{option.title}</span>
                      <span className="pw-muted">{option.hint}</span>
                    </span>
                    <span className="pw-option-check" aria-hidden="true">
                      {chosen === option.value ? <CheckIcon /> : null}
                    </span>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            // Only one choice: said, not offered.
            <p className="pw-copy-only">
              <strong>{options[0]?.title}</strong>
              <span className="pw-muted">{options[0]?.hint}</span>
            </p>
          )
        ) : plan.isError ? null : (
          <ActivityIndicator label={vi.pages.loading} />
        )}
        <p className="pw-muted">{t.hint}</p>
        {failure && !fields.planDate && !fields.name ? <ErrorBanner title={messageFor(failure)} /> : null}
      </div>
    </Sheet>
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
