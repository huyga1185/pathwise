import { useState, type ReactNode } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { useGeocodingSearch } from '@/data/geocoding';
import { formatCoordinates } from '@/features/store';
import { PinMap, type LatLng } from '@/platform/map';
import { messageFor } from '@/shared/errors';
import { formatDateLong } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { statusInfo } from '@/shared/status';
import {
  Button,
  DataTable,
  ErrorBanner,
  ProgressBar,
  StatusPill,
  TextField,
  Toast,
  type DataTableColumn,
} from '@/shared/ui';
import { PinPickerDialog } from '@/shared/ui/PinPicker';
import { allStops, usePlan, usePlanApi, type PlanDetail, type PlanStop } from './planApi';
import { ResultView } from './ResultView';
import { TrackView } from './TrackView';
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
  if (plan.isError) {
    return (
      <section className="pw-page-section">
        <ErrorBanner title={messageFor(plan.error)} />
        <Link to="/plans">{vi.plans.back}</Link>
      </section>
    );
  }
  if (!plan.data) return <p className="pw-muted">{vi.pages.loading}</p>;
  const data = plan.data;
  const view =
    data.status === 'DRAFT' && !data.optimizedAt ? (
      <GeocodeView plan={data} />
    ) : data.status === 'DRAFT' ? (
      <ResultView plan={data} onPublished={(drivers) => setToast(vi.publish.done(drivers))} />
    ) : (
      <TrackView plan={data} updatedAt={plan.dataUpdatedAt} />
    );
  return (
    <>
      {view}
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
        <Link to="/plans" className="pw-back-link">
          {vi.plans.back}
        </Link>
        <div className="pw-plan-title">
          <h1 className="pw-page-title">{plan.name}</h1>
          <StatusPill kind="plan" status={plan.status} />
          <StatusPill kind="processing" status={plan.processingStatus} />
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
  const planApi = usePlanApi();
  const stops = allStops(plan).sort((a, b) => a.id - b.id);
  const summary = plan.geocodingSummary;
  const missing = summary.pending + summary.failed;
  const busy = plan.processingStatus !== 'IDLE';
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [working, setWorking] = useState(false);
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

  const run = async (action: () => Promise<unknown>) => {
    setFailure(null);
    setWorking(true);
    try {
      await action();
    } catch (error) {
      setFailure(error);
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
        statusInfo('geocode', row.geocodeStatus) ? (
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
              <Button variant="secondary" disabled={busy} onClick={() => navigate(`/plans/${plan.id}/edit`)}>
                {g.editStops}
              </Button>
              <Button
                variant="primary"
                disabled={!canOptimize || working}
                aria-describedby="optimize-why"
                onClick={() => void run(() => planApi.optimize(plan.id, plan.version))}
              >
                {g.optimize}
              </Button>
            </div>
            <span id="optimize-why" className="pw-plans-why">
              {why}
            </span>
          </div>
        }
      />

      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}

      {plan.processingStatus === 'GEOCODING' ? (
        <section className="pw-plans-progress" aria-label={statusInfo('processing', 'GEOCODING')?.label}>
          <ProgressBar label={g.progressLabel} value={summary.total - summary.pending} max={summary.total} />
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
            onClick={() => void run(() => planApi.geocode(plan.id, plan.version))}
          >
            {g.retryAll}
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
  const [working, setWorking] = useState(false);
  const current = stop.lat !== null && stop.lon !== null ? { lat: stop.lat, lon: stop.lon } : null;
  const geocodeMessage =
    stop.geocodeStatus === 'FAILED'
      ? stop.geocodeError === 'PROVIDER_ERROR'
        ? g.providerError
        : g.notFound
      : undefined;

  const run = async (action: () => Promise<unknown>, done?: string) => {
    setFailure(null);
    setSaved(null);
    setWorking(true);
    try {
      await action();
      if (done) setSaved(done);
    } catch (error) {
      setFailure(error);
      if (error instanceof ApiError && (error.code === 'PLAN_MODIFIED' || error.code === 'PLAN_BUSY')) {
        await planApi.reload(plan.id);
      }
    } finally {
      setWorking(false);
    }
  };

  const retry = () => {
    const next = address.trim();
    // A changed address is geocoded again by the server (PENDING); the same one needs POST /geocode.
    return next && next !== (stop.address ?? '')
      ? run(() => planApi.patchStop(plan.id, stop.id, { version: plan.version, address: next }))
      : run(() => planApi.geocode(plan.id, plan.version));
  };

  const savePin = (value: LatLng) =>
    run(
      () => planApi.patchStop(plan.id, stop.id, { version: plan.version, lat: value.lat, lon: value.lon }),
      g.pinSaved(formatCoordinates(value), stop.label),
    ).then(() => setPin(null));

  const disabled = busy || working;
  return (
    <section className="pw-card pw-geocode-fix" aria-labelledby="fix-title">
      <div className="pw-geocode-fix-head">
        <h2 id="fix-title">{g.fixTitle(stopTitle(stop))}</h2>
        <StatusPill kind="geocode" status={stop.geocodeStatus} />
      </div>
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <TextField
        label={g.address}
        maxLength={500}
        value={address}
        disabled={disabled}
        error={address === (stop.address ?? '') ? geocodeMessage : undefined}
        onChange={(event) => setAddress(event.target.value)}
      />
      <div>
        <Button variant="secondary" busy={working && !pin} disabled={disabled} onClick={() => void retry()}>
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
        <Button variant="quiet" disabled={!pin || working} onClick={() => setPin(null)}>
          {g.skip}
        </Button>
        <Button
          variant="secondary"
          busy={working && pin !== null}
          disabled={!pin || disabled}
          onClick={() => pin && void savePin(pin)}
        >
          {g.savePin}
        </Button>
      </div>
      {enlarge ? (
        <PinPickerDialog
          open
          initial={pin ?? current ?? plan.depot}
          search={search}
          pinLabel={g.pinLabel}
          onConfirm={(value) => {
            setPin(value);
            setEnlarge(false);
          }}
          onClose={() => setEnlarge(false)}
        />
      ) : null}
    </section>
  );
}
