import { useStoreReadOnly } from '@/data/meQuery';
import { useState, type ReactNode } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { useGeocodingSearch } from '@/data/geocoding';
import { formatCoordinates } from '@/features/store';
import { PinMap, type LatLng } from '@/platform/map';
import { messageFor } from '@/shared/errors';
import { formatDateLong } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { BackLink } from '@/shared/ui/BackLink';
import { statusInfo } from '@/shared/status';
import {
  Button,
  DataTable,
  ErrorBanner,
  ActivityText,
  ProgressBar,
  StatusPill,
  TextField,
  Toast,
  type DataTableColumn,
  ActivityIndicator,
} from '@/shared/ui';
import { PinPickerDialog } from '@/shared/ui/PinPicker';
import { allStops, usePlan, usePlanApi, type PlanDetail, type PlanStop } from './planApi';
import { ResultView } from './ResultView';
import { TrackView } from './TrackView';
import { CopySheet, type CopiedNotice } from './CopySheet';
import './plans.css';

const g = vi.geocode;

/**
 * `/plans/:id` is one route (SCREENS §1): the view follows the plan's state, so a shared link is always right. DRAFT
 * not optimized → finding coordinates; DRAFT optimized → result; PUBLISHED and later → tracking.
 */
export function PlanDetailPage() {
  const id = Number(useParams().id);
  const plan = usePlan(id);
  const [toast, setToast] = useState<string | null>(null);
  const location = useLocation();
  const navigate = useNavigate();
  const copied = (location.state as { copied?: CopiedNotice } | null)?.copied ?? null;
  if (plan.isError) {
    return (
      <section className="pw-page-section">
        <ErrorBanner title={messageFor(plan.error)} />
        <BackLink to="/plans" label={vi.plans.title} />
      </section>
    );
  }
  if (!plan.data) return <ActivityIndicator label={vi.pages.loading} className="is-page" />;
  const data = plan.data;
  const kind = data.status === 'DRAFT' && !data.optimizedAt ? 'geocode' : data.status === 'DRAFT' ? 'result' : 'track';
  const view =
    data.status === 'DRAFT' && !data.optimizedAt ? (
      <GeocodeView plan={data} />
    ) : data.status === 'DRAFT' ? (
      <ResultView
        plan={data}
        onPublished={(drivers) => setToast(vi.publish.done(drivers))}
        onSaved={() => setToast(vi.manual.saved)}
      />
    ) : (
      <TrackView plan={data} updatedAt={plan.dataUpdatedAt} />
    );
  return (
    <>
      {copied ? <CopiedBanner notice={copied} onClose={() => navigate('.', { replace: true, state: null })} /> : null}
      <ViewSwitch kind={kind}>{view}</ViewSwitch>
      {toast ? (
        <div className="pw-toast-dock">
          <Toast tone="success" onClose={() => setToast(null)}>
            {toast}
          </Toast>
        </div>
      ) : null}
    </>
  );
}

function PlanHeader({ plan, subtitle, actions }: { plan: PlanDetail; subtitle: string; actions?: ReactNode }) {
  return (
    <div className="pw-plans-head">
      <div className="pw-plans-intro">
        <BackLink to="/plans" label={vi.plans.title} />
        <div className="pw-plan-title">
          <h1 className="pw-page-title">{plan.name}</h1>
          <StatusPill kind="plan" status={plan.status} />
          {/* A running task turns, it is not a static label (HIG Progress indicators). Finding coordinates has its
              progress bar under the title, so the title says nothing more; optimizing has none here. */}
          {plan.processingStatus === 'OPTIMIZING' ? <ActivityText text={g.optimizing} /> : null}
        </div>
        <p className="pw-muted">{subtitle}</p>
      </div>
      {actions}
    </div>
  );
}

/** Prototype "Soạn plan — tìm toạ độ" (SCREENS §1.1). */
function GeocodeView({ plan }: { plan: PlanDetail }) {
  const navigate = useNavigate();
  const { readOnly } = useStoreReadOnly();
  const planApi = usePlanApi();
  const stops = allStops(plan).sort((a, b) => a.id - b.id);
  const summary = plan.geocodingSummary;
  const missing = summary.pending + summary.failed;
  const busy = plan.processingStatus !== 'IDLE';
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [tries, setTries] = useState(0);
  const [repeat, setRepeat] = useState(false);
  const [working, setWorking] = useState(false);
  const [copying, setCopying] = useState(false);
  const selected =
    stops.find((stop) => stop.id === selectedId) ?? stops.find((stop) => stop.geocodeStatus === 'FAILED') ?? stops[0];

  const why = busy
    ? plan.processingStatus === 'OPTIMIZING'
      ? g.whyOptimizing
      : g.whyBusy
    : stops.length === 0
      ? g.whyNoStops
      : plan.routes.length === 0
        ? g.whyNoAssignments
        : missing > 0
          ? g.whyMissing(missing)
          : g.whyReady;
  const canOptimize = !busy && stops.length > 0 && plan.routes.length > 0 && missing === 0;

  // The last error stays while the next try runs and is replaced in one go by its result: taking it away first made
  // the page shorter and jump (scrolled to the bottom, the page moved up and down: owner, 2026-11-18). A new try that
  // fails again draws its banner anew (key), so it is announced again.
  const run = async (action: () => Promise<unknown>) => {
    const shownBefore = failure !== null;
    setWorking(true);
    try {
      await action();
      setFailure(null);
    } catch (error) {
      setFailure(error);
      setRepeat(shownBefore);
      setTries((n) => n + 1);
      if (error instanceof ApiError && (error.code === 'PLAN_MODIFIED' || error.code === 'PLAN_BUSY')) {
        await planApi.reload(plan.id);
      }
    } finally {
      setWorking(false);
    }
  };

  const columns: DataTableColumn<PlanStop>[] = [
    {
      key: 'order',
      header: g.order,
      render: (row) => (
        <button
          type="button"
          className="pw-stop-pick"
          aria-pressed={row.id === selected?.id}
          onClick={(event) => {
            event.stopPropagation();
            setSelectedId(row.id);
          }}
        >
          {stopTitle(row)}
        </button>
      ),
    },
    {
      key: 'address',
      header: g.address,
      render: (row) => (
        <>
          {row.address ? <div>{row.address}</div> : <div className="pw-muted">{g.noAddress}</div>}
          {row.geocodeStatus === 'RESOLVED' && row.geocodedDisplayName ? (
            <div className="pw-plans-found">{g.found(row.geocodedDisplayName)}</div>
          ) : null}
        </>
      ),
    },
    {
      key: 'window',
      header: g.window,
      numeric: true,
      render: (row) => (row.timeWindow ? `${row.timeWindow.start}–${row.timeWindow.end}` : '—'),
    },
    {
      key: 'geo',
      header: g.coordinates,
      render: (row) =>
        row.geocodeStatus === 'PENDING' ? (
          <ActivityText text={g.finding} />
        ) : statusInfo('geocode', row.geocodeStatus) ? (
          <StatusPill kind="geocode" status={row.geocodeStatus} />
        ) : (
          <span className="pw-muted">{row.geocodeStatus === 'RESOLVED' ? g.resolved : g.provided}</span>
        ),
    },
  ];

  return (
    <section className="pw-page-section" aria-labelledby="plan-title">
      <PlanHeader
        plan={plan}
        subtitle={g.subtitle(formatDateLong(plan.planDate), stops.length, plan.routes.length)}
        actions={
          <div className="pw-plan-header-actions">
            <div className="pw-plan-actions">
              {/* Copy, in every state (API_SPEC: any status); the list's touch-and-hold menu offers it too. */}
              {!readOnly ? (
                <Button variant="quiet" onClick={() => setCopying(true)}>
                  {vi.planCopy.action}
                </Button>
              ) : null}
              <Button
                variant="secondary"
                disabled={busy || readOnly}
                onClick={() => navigate(`/plans/${plan.id}/edit`)}
              >
                {g.editStops}
              </Button>
              <Button
                variant="primary"
                disabled={!canOptimize || working || readOnly}
                aria-describedby="optimize-why"
                onClick={() => void run(() => planApi.optimize(plan.id, plan.version))}
              >
                {g.optimize}
              </Button>
            </div>
            {/* Read with the button (aria-describedby); on screen each reason has its own place: the block below for
                what to do, the progress and spinners for a running task (§9.7, owner's trial 2026-11-18). */}
            <span id="optimize-why" className="pw-visually-hidden">
              {why}
            </span>
          </div>
        }
      />

      {failure ? <ErrorBanner key={tries} repeat={repeat} title={messageFor(failure)} /> : null}

      {plan.processingStatus === 'GEOCODING' ? (
        <section className="pw-plans-progress" aria-label={statusInfo('processing', 'GEOCODING')?.label}>
          {/* The bar only moves when the plan is read again: a spinner beside it keeps moving meanwhile (HIG Progress
              indicators: "Keep progress indicators moving"; macOS: a spinner "next to a specific control"). */}
          <div className="pw-progress-running">
            <ActivityText text={g.progressLabel} />
            <ProgressBar label={g.progressLabel} value={summary.total - summary.pending} max={summary.total} />
          </div>
          <p className="pw-muted">{g.progressNote}</p>
        </section>
      ) : null}

      {!busy && summary.failed > 0 ? (
        <section className="pw-plans-missing" aria-labelledby="geo-alert">
          <div className="pw-plans-missing-text">
            <h2 id="geo-alert">{g.missingTitle(missing)}</h2>
            <p className="pw-muted pw-tabular">
              {g.summary(summary.resolved, summary.provided, summary.failed, summary.pending, summary.total)}
            </p>
          </div>
          <Button
            variant="secondary"
            busy={working}
            busyLabel={g.searching}
            disabled={readOnly}
            onClick={() => void run(() => planApi.geocode(plan.id, plan.version))}
          >
            {g.retryAll}
          </Button>
        </section>
      ) : null}

      {!busy && stops.length > 0 && plan.routes.length === 0 ? (
        <section className="pw-plans-missing is-todo" aria-labelledby="assign-alert">
          <div className="pw-plans-missing-text">
            <h2 id="assign-alert">{g.noAssignmentsTitle}</h2>
            <p className="pw-muted">{g.noAssignmentsNote}</p>
          </div>
          <Button variant="secondary" disabled={readOnly} onClick={() => navigate(`/plans/${plan.id}/edit`)}>
            {g.addAssignments}
          </Button>
        </section>
      ) : null}

      <div className="pw-geocode-layout">
        <section className="pw-geocode-list" aria-label={g.tableLabel}>
          <DataTable
            caption={g.caption}
            columns={columns}
            rows={stops}
            selectedKey={selected?.id ?? null}
            onRowClick={(row) => setSelectedId(row.id)}
            className="pw-geocode-table"
          />
        </section>
        {selected ? <FixStop key={selected.id} plan={plan} stop={selected} busy={busy} /> : null}
      </div>
      {copying ? <CopySheet planId={plan.id} planName={plan.name} open onClose={() => setCopying(false)} /> : null}
    </section>
  );
}

function stopTitle(stop: PlanStop): string {
  return stop.recipientName ? `${stop.label} · ${stop.recipientName}` : stop.label;
}

/** The panel to fix one stop: a new address (geocoded by the server) or a pin (coordinates given). */
function FixStop({ plan, stop, busy }: { plan: PlanDetail; stop: PlanStop; busy: boolean }) {
  const planApi = usePlanApi();
  const search = useGeocodingSearch();
  const [address, setAddress] = useState(stop.address ?? '');
  const [pin, setPin] = useState<LatLng | null>(null);
  const [enlarge, setEnlarge] = useState(false);
  const [saved, setSaved] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [tries, setTries] = useState(0);
  const [repeat, setRepeat] = useState(false);
  // Which request runs: its own button shows the wait; nothing else changes while it runs (below).
  const [working, setWorking] = useState<'retry' | 'pin' | null>(null);
  const current = stop.lat !== null && stop.lon !== null ? { lat: stop.lat, lon: stop.lon } : null;
  const geocodeMessage =
    stop.geocodeStatus === 'FAILED'
      ? stop.geocodeError === 'PROVIDER_ERROR'
        ? g.providerError
        : g.notFound
      : undefined;

  // As the page's: the last error stays until this try's result replaces it (no jump), drawn anew when it fails again.
  const run = async (kind: 'retry' | 'pin', action: () => Promise<unknown>, done?: string) => {
    const shownBefore = failure !== null;
    // A second press while one request runs is ignored, not shown as a disabled button.
    if (working) return;
    setSaved(null);
    setWorking(kind);
    try {
      await action();
      setFailure(null);
      if (done) setSaved(done);
    } catch (error) {
      setFailure(error);
      setRepeat(shownBefore);
      setTries((n) => n + 1);
      if (error instanceof ApiError && (error.code === 'PLAN_MODIFIED' || error.code === 'PLAN_BUSY')) {
        await planApi.reload(plan.id);
      }
    } finally {
      setWorking(null);
    }
  };

  const retry = () => {
    const next = address.trim();
    // A changed address is geocoded again by the server (PENDING); the same one needs POST /geocode.
    return next && next !== (stop.address ?? '')
      ? run('retry', () => planApi.patchStop(plan.id, stop.id, { version: plan.version, address: next }))
      : run('retry', () => planApi.geocode(plan.id, plan.version));
  };

  const savePin = (value: LatLng) =>
    run(
      'pin',
      () => planApi.patchStop(plan.id, stop.id, { version: plan.version, lat: value.lat, lon: value.lon }),
      g.pinSaved(formatCoordinates(value), stop.label),
    ).then(() => setPin(null));

  // Only the server's own processing locks the panel. A short request does not: the map, its controls and the
  // buttons stay as they are (HIG Loading: "Let people do other things… while they wait"); disabling them for the
  // ~0.15 s of a request made them flash (owner, 2026-11-18).
  const disabled = busy;
  return (
    // A new stop chosen: the panel's content fades in (§9.6, owner's answer: a light crossfade, no movement).
    <section className="pw-card pw-geocode-fix is-switching" aria-labelledby="fix-title">
      <div className="pw-geocode-fix-head">
        <h2 id="fix-title">{g.fixTitle(stopTitle(stop))}</h2>
        {stop.geocodeStatus === 'PENDING' ? (
          <ActivityText text={g.finding} />
        ) : (
          <StatusPill kind="geocode" status={stop.geocodeStatus} />
        )}
      </div>
      {failure ? <ErrorBanner key={tries} repeat={repeat} title={messageFor(failure)} /> : null}
      <TextField
        label={g.address}
        maxLength={500}
        value={address}
        disabled={disabled}
        error={address === (stop.address ?? '') ? geocodeMessage : undefined}
        onChange={(event) => setAddress(event.target.value)}
      />
      <div className="pw-inline-action">
        <Button
          variant="secondary"
          disabled={disabled}
          busy={working === 'retry'}
          busyLabel={g.searching}
          onClick={() => void retry()}
        >
          {g.retryOne}
        </Button>
      </div>
      <div className="pw-or" aria-hidden="true">
        <span />
        {g.orPin}
        <span />
      </div>
      <div className="pw-geocode-map">
        <PinMap
          value={pin ?? current}
          onChange={disabled ? undefined : setPin}
          readOnly={disabled}
          className="pw-map-4x3"
          labels={{
            zoomIn: vi.map.zoomIn,
            zoomOut: vi.map.zoomOut,
            pin: g.pinLabel,
            placeAtCenter: vi.map.placeAtCenter,
          }}
        />
        <Button variant="secondary" disabled={disabled} onClick={() => setEnlarge(true)}>
          {g.enlarge}
        </Button>
      </div>
      <p role="status" className="pw-muted pw-tabular">
        {saved ?? (pin ? g.pinAt(formatCoordinates(pin)) : g.pinHint)}
      </p>
      <div className="pw-plan-actions pw-geocode-fix-actions">
        <Button variant="quiet" disabled={!pin} onClick={() => setPin(null)}>
          {g.skip}
        </Button>
        <Button
          variant="secondary"
          busy={working === 'pin'}
          busyLabel={g.savingPin}
          disabled={!pin || disabled}
          onClick={() => pin && void savePin(pin)}
        >
          {g.savePin}
        </Button>
      </div>
      {/* Always there, so it closes with its animation (Dialog). */}
      <PinPickerDialog
        open={enlarge}
        initial={pin ?? current ?? plan.depot}
        search={search}
        pinLabel={g.pinLabel}
        onConfirm={(value) => {
          setPin(value);
          setEnlarge(false);
        }}
        onClose={() => setEnlarge(false)}
      />
    </section>
  );
}

/** Once, after a copy (W12, prototype "Kế hoạch — sao chép"): what was copied and the assignments left out. */
function CopiedBanner({ notice, onClose }: { notice: CopiedNotice; onClose: () => void }) {
  return (
    <section role="status" className="pw-copy-notice">
      <div className="pw-copy-notice-text">
        <strong>{vi.planCopy.done(notice.from, notice.stops, notice.all)}</strong>
        {notice.skipped.length > 0 ? <span>{vi.planCopy.skipped(notice.skipped.join(', '))}</span> : null}
      </div>
      <Button variant="quiet" onClick={onClose}>
        {vi.planCopy.close}
      </Button>
    </section>
  );
}

/**
 * The plan's screen changes in place (finding coordinates → optimized result → tracking): the new one comes in as a
 * page does (OWNER_UI_REDESIGN §6.2 CĐ2b: fades in and rises 12 px), so the change is seen, not a jump. Not on the
 * first view: the page itself has just come in.
 */
function ViewSwitch({ kind, children }: { kind: string; children: ReactNode }) {
  const [first] = useState(kind);
  const [moved, setMoved] = useState(false);
  if (!moved && kind !== first) setMoved(true);
  return (
    <div key={kind} className={moved ? 'pw-view-switch is-new' : 'pw-view-switch'}>
      {children}
    </div>
  );
}
