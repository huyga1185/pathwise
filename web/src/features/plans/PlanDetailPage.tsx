import { useStoreReadOnly } from '@/data/meQuery';
import { useEffect, useRef, useState, type ReactNode } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { formatCoordinates } from '@/features/store';
import { PinMap, RouteMap, type LatLng } from '@/platform/map';
import { messageFor } from '@/shared/errors';
import { formatDateLong } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { BackLink } from '@/shared/ui/BackLink';
import { statusInfo } from '@/shared/status';
import {
  Button,
  ErrorBanner,
  ActivityText,
  MapStage,
  ProgressBar,
  StatusPill,
  TextField,
  Toast,
  ActivityIndicator,
  useMapStage,
} from '@/shared/ui';
import { ChevronLeftIcon } from '@/shared/ui/icons';
import { useMediaQuery } from '@/shared/hooks/useMediaQuery';
import { allStops, usePlan, usePlanApi, type PlanDetail, type PlanStop } from './planApi';
import { ResultView } from './ResultView';
import { TrackView } from './TrackView';
import { CopySheet, type CopiedNotice } from './CopySheet';
import { RouteGroup } from './PlanStage';
import { MAP_LABELS } from './planShared';
import './plans.css';

/** Narrower than this, the three header buttons no longer fit one row (measured 2026-11-18): they stack. */
const STACK_QUERY = '(max-width: 439px)';

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

/** Prototype "Soạn plan — tìm toạ độ" (SCREENS §1.1). */
function GeocodeView({ plan }: { plan: PlanDetail }) {
  const navigate = useNavigate();
  const { readOnly } = useStoreReadOnly();
  const planApi = usePlanApi();
  const stops = allStops(plan).sort((a, b) => a.id - b.id);
  const summary = plan.geocodingSummary;
  const missing = summary.pending + summary.failed;
  const busy = plan.processingStatus !== 'IDLE';
  // The first stop not found opens when the page opens (SCREENS §1.1); back to the list does not open it again.
  const [selectedId, setSelectedId] = useState<number | null>(
    () => allStops(plan).find((stop) => stop.geocodeStatus === 'FAILED')?.id ?? null,
  );
  const [failure, setFailure] = useState<unknown>(null);
  const [tries, setTries] = useState(0);
  const [repeat, setRepeat] = useState(false);
  const [working, setWorking] = useState(false);
  const [copying, setCopying] = useState(false);

  const stacked = useMediaQuery(STACK_QUERY);
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

  const optimizeButton = (
    <Button
      variant="primary"
      disabled={!canOptimize || working || readOnly}
      aria-describedby="optimize-why"
      onClick={() => void run(() => planApi.optimize(plan.id, plan.version))}
    >
      {g.optimize}
    </Button>
  );
  const [pin, setPin] = useState<LatLng | null>(null);
  const editing = stops.find((stop) => stop.id === selectedId) ?? null;
  const current =
    editing && editing.lat !== null && editing.lon !== null ? { lat: editing.lat, lon: editing.lon } : null;
  const placed = stops.flatMap((stop) =>
    stop.lat !== null && stop.lon !== null
      ? [{ id: stop.id, lat: stop.lat, lon: stop.lon, label: stopTitle(stop), sequence: null }]
      : [],
  );
  // Focus moves into a card people open; the one open when the page loads leaves focus where the page starts.
  const [opened, setOpened] = useState(false);
  const open = (id: number) => {
    setPin(null);
    setOpened(true);
    setSelectedId(id);
  };
  const back = () => {
    const id = selectedId;
    setPin(null);
    setSelectedId(null);
    window.requestAnimationFrame(() => document.querySelector<HTMLElement>(`[data-stop="${id}"]`)?.focus());
  };
  const status = (row: PlanStop) =>
    row.geocodeStatus === 'PENDING' ? (
      <ActivityText text={g.finding} />
    ) : statusInfo('geocode', row.geocodeStatus) ? (
      <StatusPill kind="geocode" status={row.geocodeStatus} />
    ) : (
      <span className="pw-muted">{row.geocodeStatus === 'RESOLVED' ? g.resolved : g.provided}</span>
    );

  return (
    <MapStage
      label={g.tableLabel}
      map={(inset) =>
        editing && !busy && !readOnly ? (
          // Fixing a stop: the whole map places its pin (G5.15, G5-Q11): click, drag, or "Đặt ghim ở giữa bản đồ".
          <PinMap
            key={`pin-${editing.id}`}
            value={pin ?? current}
            start={plan.depot}
            kind="stop"
            onChange={setPin}
            labels={{
              zoomIn: vi.map.zoomIn,
              zoomOut: vi.map.zoomOut,
              pin: g.pinLabel,
              placeAtCenter: vi.map.placeAtCenter,
            }}
          />
        ) : (
          <RouteMap
            staged
            inset={inset}
            depot={{ lat: plan.depot.lat, lon: plan.depot.lon }}
            routes={[]}
            unassigned={placed}
            selectedId={selectedId}
            onSelect={(id) => open(Number(id))}
            labels={MAP_LABELS}
          />
        )
      }
    >
      <div className="pw-stage-head" data-peek>
        <BackLink to="/plans" label={vi.plans.title} />
        <div className="pw-plan-title">
          <h1 id="plan-title" className="pw-page-title pw-stage-title">
            {plan.name}
          </h1>
          <StatusPill kind="plan" status={plan.status} />
        </div>
        <p className="pw-muted">{g.subtitle(formatDateLong(plan.planDate), stops.length, plan.routes.length)}</p>
        {/* One row: edit, copy, the primary action last (HIG Toolbars). Too narrow for one row (< 440px, measured):
            stacked full width, the primary action first (§9.12) — in that order in the page too, so reading and Tab
            follow what is seen (WCAG 1.3.2, 2.4.3). */}
        <div className={stacked ? 'pw-plan-actions is-stacked' : 'pw-plan-actions pw-stage-actions'}>
          {stacked ? optimizeButton : null}
          <Button variant="secondary" disabled={busy || readOnly} onClick={() => navigate(`/plans/${plan.id}/edit`)}>
            {g.editStops}
          </Button>
          {/* Copy, in every state (API_SPEC: any status). Tinted, between the grey edit and the primary action
              (HIG Toolbars; §9.10). */}
          {!readOnly ? (
            <Button variant="tinted" onClick={() => setCopying(true)}>
              {vi.planCopy.action}
            </Button>
          ) : null}
          {stacked ? null : optimizeButton}
        </div>
        {/* Read with the button (aria-describedby); on screen each reason has its own place (§9.7). */}
        <span id="optimize-why" className="pw-visually-hidden">
          {why}
        </span>
      </div>

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

      {editing ? (
        <FixStop
          key={editing.id}
          plan={plan}
          stop={editing}
          busy={busy}
          pin={pin}
          onPin={setPin}
          onBack={back}
          takeFocus={opened}
        />
      ) : (
        <div className="pw-stage-list">
          <RouteGroup
            title={g.tableLabel}
            summary={g.listSummary(stops.length)}
            numbered={false}
            stops={stops}
            trailing={status}
            onOpen={(stop) => open(stop.id)}
          />
        </div>
      )}
      {copying ? <CopySheet planId={plan.id} planName={plan.name} open onClose={() => setCopying(false)} /> : null}
    </MapStage>
  );
}

function stopTitle(stop: PlanStop): string {
  return stop.recipientName ? `${stop.label} · ${stop.recipientName}` : stop.label;
}

/**
 * Fixing one stop (G5.15): a card in the panel / sheet — a new address (geocoded by the server) or a pin placed on
 * the full map (G5-Q11: no small map, no "enlarge"); back returns to the list.
 */
function FixStop({
  plan,
  stop,
  busy,
  pin,
  onPin,
  onBack,
  takeFocus,
}: {
  plan: PlanDetail;
  stop: PlanStop;
  busy: boolean;
  pin: LatLng | null;
  onPin: (value: LatLng | null) => void;
  onBack: () => void;
  /** Opened by people (not the one open when the page loads): focus goes to its title (WCAG 2.4.3). */
  takeFocus: boolean;
}) {
  const planApi = usePlanApi();
  const stage = useMapStage();
  const heading = useRef<HTMLHeadingElement>(null);
  const [address, setAddress] = useState(stop.address ?? '');
  const [saved, setSaved] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [tries, setTries] = useState(0);
  const [repeat, setRepeat] = useState(false);
  // Which request runs: its own button shows the wait; nothing else changes while it runs (below).
  const [working, setWorking] = useState<'retry' | 'pin' | null>(null);
  useEffect(() => {
    if (takeFocus) heading.current?.focus({ preventScroll: true });
    // The panel / sheet may be scrolled down the list: show the card from its top.
    heading.current?.closest('section')?.scrollIntoView?.({ block: 'start' });
    // On a phone the sheet leaves half the map in sight to place the pin.
    if (stage && !stage.wide) stage.setDetent('mid');
    // Once per stop opened.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [stop.id]);
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
    ).then(() => onPin(null));

  // Only the server's own processing locks the card; a short request leaves everything as it is (HIG Loading: "Let
  // people do other things… while they wait"; §9.5).
  const disabled = busy;
  return (
    <section className="pw-stop-card is-switching" aria-labelledby="fix-title">
      <button type="button" className="pw-back-link pw-stop-card-back" onClick={onBack}>
        <ChevronLeftIcon />
        <span>{g.tableLabel}</span>
      </button>
      <div className="pw-geocode-fix-head">
        <h2 id="fix-title" ref={heading} tabIndex={-1} className="pw-stop-card-title">
          {g.fixTitle(stopTitle(stop))}
        </h2>
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
        // Phone: typing raises the sheet so the keyboard does not cover the field and its button (WCAG 2.4.11);
        // leaving the field keeps the detent (pull it down to place a pin).
        onFocus={() => {
          if (stage && !stage.wide) stage.setDetent('high');
        }}
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
      <p role="status" className="pw-muted pw-tabular">
        {saved ?? (pin ? g.pinAt(formatCoordinates(pin)) : g.pinHint)}
      </p>
      <div className="pw-plan-actions pw-geocode-fix-actions">
        <Button variant="quiet" disabled={!pin} onClick={() => onPin(null)}>
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
