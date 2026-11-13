import { useState } from 'react';
import { Link, useBlocker, useNavigate } from 'react-router';
import { ApiError } from '@/data/apiError';
import { RouteMap } from '@/platform/map';
import { jobMessageFor, messageFor } from '@/shared/errors';
import { formatDateLong, formatDayTime, formatDistance, formatDuration, formatTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, Dialog, ErrorBanner, ProgressBar, StatusPill } from '@/shared/ui';
import { ManualBoard } from './ManualBoard';
import {
  draftChanged,
  draftFits,
  draftOf,
  draftPlan,
  lateMinutes,
  replaceRequest,
  violationStop,
  violationText,
  type Draft,
} from './manualEdit';
import { CancelDialog, PublishDialog, routesWithStops } from './PlanDialogs';
import { allStops, useJob, usePlanApi, type Job, type PlanDetail, type PlanStop } from './planApi';
import {
  driverName,
  loadText,
  MAP_LABELS,
  mapData,
  RouteBadge,
  spanText,
  StatCard,
  stopPlace,
  stopTitle,
  vehicleText,
  windowText,
  type PlanRoute,
} from './planShared';

const r = vi.result;
const m = vi.manual;

/**
 * Prototype "Soạn plan — sau khi tối ưu" (SCREENS §1.1): overview, the stops the solver could not place (a warning,
 * publishing is still allowed), one collapsible block per route with its table, and the map; a row and its marker
 * select each other. Re-optimize follows `OPTIMIZING` by polling; a failed job keeps the previous result.
 * "Chỉnh tay" (W10, P-58) turns the routes into drag-and-drop columns; saving recomputes times and distances on the
 * server, which reports the constraints the edit breaks (`violations`).
 */
export function ResultView({
  plan,
  onPublished,
  onSaved,
}: {
  plan: PlanDetail;
  onPublished: (drivers: number) => void;
  onSaved: () => void;
}) {
  const navigate = useNavigate();
  const planApi = usePlanApi();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [collapsed, setCollapsed] = useState<Record<number, boolean>>({});
  const [job, setJob] = useState<Job | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [dialog, setDialog] = useState<'publish' | 'cancel' | 'discard' | 'reoptimize' | null>(null);
  const [savedDraft, setDraft] = useState<Draft | null>(null);
  // The plan changed under the draft (another dispatcher, a new optimization): a draft that no longer fits is dropped.
  const draft = savedDraft !== null && draftFits(plan, savedDraft) ? savedDraft : null;
  const [saving, setSaving] = useState(false);
  // A sentence of the manual edit (UX_BRIEF §6.3) rather than an error code.
  const [notice, setNotice] = useState<string | null>(null);
  const editing = draft !== null;
  const changed = draft !== null && draftChanged(plan, draft);
  // Leaving the page with an unsaved draft asks first.
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) => changed && currentLocation.pathname !== nextLocation.pathname,
  );
  const jobState = useJob(job);
  const busy = plan.processingStatus !== 'IDLE';
  const stops = allStops(plan);
  const routed = plan.routes.reduce((sum, route) => sum + route.stops.length, 0);
  const drivers = routesWithStops(plan).length;
  const infeasible = plan.unassignedStops.filter((stop) => stop.dropReason === 'INFEASIBLE');
  const takenOff = plan.unassignedStops.filter((stop) => stop.dropReason === 'MANUAL');
  const late = lateMinutes(plan);
  const shown = draft ? draftPlan(plan, draft) : plan;
  const failedJob = jobState.data?.status === 'FAILED' ? jobState.data : null;

  const save = async () => {
    if (!draft) return;
    setSaving(true);
    setFailure(null);
    setNotice(null);
    try {
      await planApi.replaceRoutes(plan.id, replaceRequest(plan, draft));
      setDraft(null);
      onSaved();
    } catch (error) {
      if (error instanceof ApiError && (error.code === 'PLAN_MODIFIED' || error.code === 'PLAN_BUSY')) {
        setNotice(m.modified);
        await planApi.reload(plan.id);
      } else if (error instanceof ApiError && error.code === 'VALIDATION_ERROR') {
        // The draft no longer matches the plan: a bug or a change the app missed.
        setNotice(m.staleList);
        setDraft(null);
        await planApi.reload(plan.id);
      } else if (error instanceof ApiError && error.code === 'ROUTING_PROVIDER_ERROR') {
        setNotice(m.recomputeFailed);
      } else {
        setFailure(error);
      }
    } finally {
      setSaving(false);
    }
  };

  const cancelEdit = () => {
    if (changed) setDialog('discard');
    else setDraft(null);
  };

  // Optimizing again replaces manual edits: ask when there is something to lose.
  const askReoptimize = () => {
    if (changed || takenOff.length > 0 || plan.violations.length > 0) setDialog('reoptimize');
    else void reoptimize();
  };

  const reoptimize = async () => {
    setDraft(null);
    setNotice(null);
    setFailure(null);
    setJob(null);
    try {
      setJob(await planApi.optimize(plan.id, plan.version));
    } catch (error) {
      setFailure(error);
      if (error instanceof ApiError && (error.code === 'PLAN_MODIFIED' || error.code === 'PLAN_BUSY')) {
        await planApi.reload(plan.id);
      }
    }
  };

  const select = (id: number) => setSelectedId(id);
  const selection = describeSelection(shown, allStops(shown).find((stop) => stop.id === selectedId) ?? null);
  const { routes, unassigned } = mapData(shown, false);

  return (
    <section className="pw-page-section" aria-labelledby="plan-title">
      <div className="pw-plans-head">
        <div className="pw-plans-intro">
          <Link to="/plans" className="pw-back-link">
            {vi.plans.back}
          </Link>
          <div className="pw-plan-title">
            <h1 id="plan-title" className="pw-page-title">
              {plan.name}
            </h1>
            <StatusPill kind="plan" status={plan.status} />
            <StatusPill kind="processing" status={plan.processingStatus} />
          </div>
          <p className="pw-muted">
            {r.subtitle(
              formatDateLong(plan.planDate),
              plan.depot.address ?? vi.map.depot,
              plan.optimizedAt ? formatDayTime(plan.optimizedAt, plan.timezone) : '—',
            )}
          </p>
        </div>
        {!editing ? (
          <div className="pw-plan-actions">
            <Button variant="quiet" disabled={busy} onClick={() => setDialog('cancel')}>
              {r.cancel}
            </Button>
            <Button variant="secondary" disabled={busy} onClick={() => navigate(`/plans/${plan.id}/edit`)}>
              {r.editStops}
            </Button>
            <Button variant="secondary" disabled={busy} onClick={() => setDraft(draftOf(plan))}>
              {m.start}
            </Button>
            <Button variant="secondary" busy={busy} onClick={askReoptimize}>
              {busy ? r.optimizing : r.reoptimize}
            </Button>
            <Button variant="primary" disabled={busy || drivers === 0} onClick={() => setDialog('publish')}>
              {r.publish(drivers)}
            </Button>
          </div>
        ) : null}
      </div>

      {editing ? (
        <section className="pw-manual-banner" aria-label={m.regionLabel}>
          <div className="pw-manual-banner-text">
            <strong>{m.bannerTitle}</strong>
            <span className="pw-muted">{m.bannerLead}</span>
          </div>
          <Button variant="quiet" disabled={saving} onClick={cancelEdit}>
            {m.cancel}
          </Button>
          <Button variant="primary" busy={saving} disabled={!changed} onClick={() => void save()}>
            {saving ? m.saving : m.save}
          </Button>
        </section>
      ) : null}

      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      {notice ? <ErrorBanner title={notice} /> : null}
      {failedJob ? <ErrorBanner title={jobMessageFor(failedJob.errorCode)}>{r.jobFailed}</ErrorBanner> : null}

      {busy ? (
        <section className="pw-plans-progress" aria-label={r.progressLabel}>
          <ProgressBar label={r.progressLabel} />
          <p className="pw-muted">{r.progressNote}</p>
        </section>
      ) : null}

      <section className="pw-stats" aria-label={r.overview}>
        <StatCard art="stops" label={r.stops} value={r.routed(routed, stops.length)} />
        <StatCard art="distance" label={r.distance} value={formatDistance(plan.totalDistanceMeters)} />
        <StatCard art="duration" label={r.duration} value={formatDuration(plan.totalDurationSeconds)} />
        <StatCard art="drivers" label={r.drivers} value={r.driversValue(drivers, drivers)} />
      </section>

      {!editing && plan.violations.length > 0 ? (
        <section className="pw-violations" aria-labelledby="violations-title">
          <h2 id="violations-title">{m.violationsTitle(plan.violations.length)}</h2>
          <p className="pw-muted">{m.violationsLead}</p>
          <ul>
            {plan.violations.map((violation, index) => (
              <li key={`${violation.code}-${violation.routeId}-${violation.stopId ?? ''}-${index}`}>
                <button
                  type="button"
                  className="pw-link-button"
                  onClick={() => {
                    const stop = violationStop(plan, violation);
                    if (stop !== null) select(stop);
                  }}
                >
                  {violationText(plan, violation)}
                </button>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      {!editing && takenOff.length > 0 ? (
        <section className="pw-infeasible" aria-labelledby="manual-title">
          <StatusPill kind="dropReason" status="MANUAL" />
          <div className="pw-infeasible-text">
            <h2 id="manual-title">{m.manualTitle(takenOff.length, takenOff.map(stopTitle).join(', '))}</h2>
            <p className="pw-muted">{m.manualLead}</p>
          </div>
          <button type="button" className="pw-link-button" onClick={() => select((takenOff[0] as PlanStop).id)}>
            {r.showOnMap}
          </button>
        </section>
      ) : null}

      {infeasible.length > 0 ? (
        <section className="pw-infeasible" aria-labelledby="unassigned-title">
          <StatusPill kind="dropReason" status="INFEASIBLE" />
          <div className="pw-infeasible-text">
            <h2 id="unassigned-title">{r.unassignedTitle(infeasible.length, infeasible.map(stopTitle).join(', '))}</h2>
            <p className="pw-muted">{r.unassignedLead}</p>
          </div>
          <button type="button" className="pw-link-button" onClick={() => select((infeasible[0] as PlanStop).id)}>
            {r.showOnMap}
          </button>
        </section>
      ) : null}

      <div className="pw-result-layout">
        <div className="pw-result-routes">
          {draft ? (
            <ManualBoard plan={plan} draft={draft} onChange={setDraft} />
          ) : (
            plan.routes.map((route, index) => (
              <RouteBlock
                key={route.id}
                route={route}
                index={index}
                timeZone={plan.timezone}
                collapsed={collapsed[route.id] === true}
                onToggle={() => setCollapsed((state) => ({ ...state, [route.id]: !state[route.id] }))}
                selectedId={selectedId}
                onSelect={select}
                violated={plan.violations.some((violation) => violation.routeId === route.id)}
                late={late}
              />
            ))
          )}
        </div>
        <section className="pw-result-map" aria-label={r.mapLabel}>
          <RouteMap
            depot={{ lat: plan.depot.lat, lon: plan.depot.lon }}
            routes={routes}
            unassigned={unassigned}
            selectedId={selectedId}
            onSelect={(id) => select(Number(id))}
            labels={MAP_LABELS}
          />
          <div role="status" className="pw-map-selection">
            {selection ? (
              <>
                <strong>{selection.title}</strong>
                <span className="pw-muted">{selection.detail}</span>
              </>
            ) : (
              <span className="pw-muted">{r.pickHint}</span>
            )}
          </div>
        </section>
      </div>

      <PublishDialog
        plan={plan}
        open={dialog === 'publish'}
        onClose={() => setDialog(null)}
        onPublished={(n) => {
          setDialog(null);
          onPublished(n);
        }}
      />
      <CancelDialog
        plan={plan}
        open={dialog === 'cancel'}
        onClose={() => setDialog(null)}
        onCancelled={() => setDialog(null)}
      />
      <Dialog
        open={dialog === 'discard'}
        title={m.discardTitle}
        confirmLabel={m.discardConfirm}
        cancelLabel={m.keepEditing}
        tone="danger"
        onConfirm={() => {
          setDialog(null);
          setDraft(null);
        }}
        onClose={() => setDialog(null)}
      >
        <p>{m.discardLead}</p>
      </Dialog>
      <Dialog
        open={dialog === 'reoptimize'}
        title={m.reoptimizeTitle}
        confirmLabel={m.reoptimizeConfirm}
        onConfirm={() => {
          setDialog(null);
          void reoptimize();
        }}
        onClose={() => setDialog(null)}
      >
        <p>{m.reoptimizeLead}</p>
      </Dialog>
      <Dialog
        open={blocker.state === 'blocked'}
        title={m.leaveTitle}
        confirmLabel={m.leaveConfirm}
        cancelLabel={m.keepEditing}
        tone="danger"
        onConfirm={() => blocker.proceed?.()}
        onClose={() => blocker.reset?.()}
      >
        <p>{m.leaveLead}</p>
      </Dialog>
    </section>
  );
}

function RouteBlock({
  route,
  index,
  timeZone,
  collapsed,
  onToggle,
  selectedId,
  onSelect,
  violated,
  late,
}: {
  route: PlanRoute;
  index: number;
  timeZone: string;
  collapsed: boolean;
  onToggle: () => void;
  selectedId: number | null;
  onSelect: (id: number) => void;
  violated: boolean;
  late: Map<number, number>;
}) {
  const tableId = `route-${route.id}-stops`;
  const load = loadText(route);
  return (
    <section className="pw-route-block" aria-label={driverName(route)}>
      <div className="pw-route-block-head">
        <button
          type="button"
          className="pw-route-toggle"
          aria-expanded={!collapsed}
          aria-controls={tableId}
          onClick={onToggle}
        >
          <RouteBadge index={index} />
          <span className="pw-route-driver">{driverName(route)}</span>
          <span className="pw-muted">{vehicleText(route)}</span>
          {violated ? <span className="pw-chip-signal">{m.violationBadge}</span> : null}
          <span className="pw-grow" />
          <span className="pw-route-toggle-label">{collapsed ? r.expand : r.collapse}</span>
        </button>
        <p className="pw-muted pw-tabular">
          {r.routeSummary(
            route.stops.length,
            formatDistance(route.plannedDistanceMeters),
            formatDuration(route.plannedDurationSeconds),
            spanText(route, timeZone),
            load ? r.load(load) : '',
          )}
        </p>
      </div>
      {!collapsed ? (
        <div className="pw-table-wrap pw-route-table" id={tableId}>
          <table className="pw-table">
            <caption className="pw-visually-hidden">{r.caption(driverName(route))}</caption>
            <thead>
              <tr>
                <th scope="col" className="pw-num pw-col-seq">
                  {r.sequence}
                </th>
                <th scope="col">{r.order}</th>
                <th scope="col">{r.address}</th>
                <th scope="col" className="pw-num">
                  {r.window}
                </th>
                <th scope="col" className="pw-num">
                  {r.eta}
                </th>
              </tr>
            </thead>
            <tbody>
              {route.stops.map((stop) => (
                <tr
                  key={stop.id}
                  className={stop.id === selectedId ? 'pw-row-clickable pw-row-selected' : 'pw-row-clickable'}
                  onClick={() => onSelect(stop.id)}
                >
                  <td className="pw-num">{stop.sequence}</td>
                  <td>
                    <button
                      type="button"
                      className="pw-stop-pick"
                      aria-pressed={stop.id === selectedId}
                      onClick={(event) => {
                        event.stopPropagation();
                        onSelect(stop.id);
                      }}
                    >
                      {stopTitle(stop)}
                    </button>
                    {stop.note ? <div className="pw-plans-found">{stop.note}</div> : null}
                  </td>
                  <td>{stop.address ?? <span className="pw-muted">{stopPlace(stop)}</span>}</td>
                  <td className="pw-num">{windowText(stop) || '—'}</td>
                  <td className="pw-num pw-eta">
                    {stop.plannedArrival ? formatTime(stop.plannedArrival, timeZone) : '—'}
                    {late.has(stop.id) ? (
                      <div>
                        <span className="pw-chip-signal">{m.late(late.get(stop.id) ?? 0)}</span>
                      </div>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}
    </section>
  );
}

/** The line under the map for the stop selected in a table or on the map. */
export function describeSelection(plan: PlanDetail, stop: PlanStop | null): { title: string; detail: string } | null {
  if (!stop) return null;
  const route = plan.routes.find((item) => item.stops.some((candidate) => candidate.id === stop.id));
  return {
    title: route ? r.selection(driverName(route), stop.sequence, stopTitle(stop)) : `${r.noRoute} · ${stopTitle(stop)}`,
    detail: r.selectionDetail(
      stopPlace(stop),
      stop.plannedArrival ? formatTime(stop.plannedArrival, plan.timezone) : '',
      windowText(stop),
    ),
  };
}
