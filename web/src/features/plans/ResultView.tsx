import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { ApiError } from '@/data/apiError';
import { RouteMap } from '@/platform/map';
import { jobMessageFor, messageFor } from '@/shared/errors';
import { formatDateLong, formatDayTime, formatDistance, formatDuration, formatTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, ErrorBanner, ProgressBar, StatusPill } from '@/shared/ui';
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

/**
 * Prototype "Soạn plan — sau khi tối ưu" (SCREENS §1.1): overview, the stops the solver could not place (a warning,
 * publishing is still allowed), one collapsible block per route with its table, and the map; a row and its marker
 * select each other. Re-optimize follows `OPTIMIZING` by polling; a failed job keeps the previous result.
 */
export function ResultView({ plan, onPublished }: { plan: PlanDetail; onPublished: (drivers: number) => void }) {
  const navigate = useNavigate();
  const planApi = usePlanApi();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [collapsed, setCollapsed] = useState<Record<number, boolean>>({});
  const [job, setJob] = useState<Job | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [dialog, setDialog] = useState<'publish' | 'cancel' | null>(null);
  const jobState = useJob(job);
  const busy = plan.processingStatus !== 'IDLE';
  const stops = allStops(plan);
  const routed = plan.routes.reduce((sum, route) => sum + route.stops.length, 0);
  const drivers = routesWithStops(plan).length;
  const infeasible = plan.unassignedStops.filter((stop) => stop.dropReason === 'INFEASIBLE');
  const failedJob = jobState.data?.status === 'FAILED' ? jobState.data : null;

  const reoptimize = async () => {
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
  const selection = describeSelection(plan, stops.find((stop) => stop.id === selectedId) ?? null);
  const { routes, unassigned } = mapData(plan, false);

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
        <div className="pw-plan-actions">
          <Button variant="quiet" disabled={busy} onClick={() => setDialog('cancel')}>
            {r.cancel}
          </Button>
          <Button variant="secondary" disabled={busy} onClick={() => navigate(`/plans/${plan.id}/edit`)}>
            {r.editStops}
          </Button>
          <Button variant="secondary" busy={busy} onClick={() => void reoptimize()}>
            {busy ? r.optimizing : r.reoptimize}
          </Button>
          <Button variant="primary" disabled={busy || drivers === 0} onClick={() => setDialog('publish')}>
            {r.publish(drivers)}
          </Button>
        </div>
      </div>

      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
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
          {plan.routes.map((route, index) => (
            <RouteBlock
              key={route.id}
              route={route}
              index={index}
              timeZone={plan.timezone}
              collapsed={collapsed[route.id] === true}
              onToggle={() => setCollapsed((state) => ({ ...state, [route.id]: !state[route.id] }))}
              selectedId={selectedId}
              onSelect={select}
            />
          ))}
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
}: {
  route: PlanRoute;
  index: number;
  timeZone: string;
  collapsed: boolean;
  onToggle: () => void;
  selectedId: number | null;
  onSelect: (id: number) => void;
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
