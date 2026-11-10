import { useState } from 'react';
import { Link } from 'react-router';
import { DriverIcon, RouteMap, routeColour, type MapDriver } from '@/platform/map';
import { formatDateLong, formatPhone, formatTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, buttonClass, StatusPill } from '@/shared/ui';
import { CancelDialog } from './PlanDialogs';
import { allStops, useDriverLocations, type DriverLocation, type PlanDetail, type PlanStop } from './planApi';
import { describeSelection } from './ResultView';
import {
  driverName,
  MAP_LABELS,
  mapData,
  RouteBadge,
  stopPlace,
  stopTitle,
  vehicleText,
  type PlanRoute,
} from './planShared';

const t = vi.track;
const loc = vi.location;
/** Selection id of a driver marker; stops are selected by their numeric id. */
const driverKey = (routeId: number) => `driver-${routeId}`;
const OUTCOMES = ['DELIVERED', 'FAILED', 'SKIPPED', 'PENDING'] as const;
type Outcome = (typeof OUTCOMES)[number];

/** Stops of a route by result: what the progress bar and the summary show (counting, not deciding a status). */
function countByResult(stops: PlanStop[]): Record<Outcome, number> {
  const count: Record<Outcome, number> = { DELIVERED: 0, FAILED: 0, SKIPPED: 0, PENDING: 0 };
  stops.forEach((stop) => {
    count[stop.status] += 1;
  });
  return count;
}

function minutesAgo(instant: string, now: number): string {
  return loc.ago(Math.max(0, Math.floor((now - Date.parse(instant)) / 60_000)));
}

/**
 * The line under a route's progress (prototype "Theo dõi — vị trí tài xế"): when its position was updated, or why
 * there is none, from the state the driver's app reported (P-53). Nothing for a finished route.
 */
function locationLine(route: PlanRoute, found: DriverLocation | undefined, timeZone: string, now: number) {
  if (route.status === 'COMPLETED') return null;
  if (found?.location) {
    const { recordedAt } = found.location;
    return { located: true, text: loc.updated(minutesAgo(recordedAt, now), formatTime(recordedAt, timeZone)) };
  }
  const state = found?.sharing?.state;
  const why = state && state !== 'SHARING' ? loc.why[state](driverName(route)) : loc.why.NO_SIGNAL();
  const phone = route.driver.phoneNumber ? ` · ${loc.call(formatPhone(route.driver.phoneNumber))}` : '';
  return { located: false, text: why + phone };
}

/** What the line under the map says about a selected driver. */
function describeDriver(route: PlanRoute, found: DriverLocation, timeZone: string, now: number) {
  const position = found.location;
  if (!position) return null;
  const ago = minutesAgo(position.recordedAt, now);
  const next = route.stops.find((stop) => stop.status === 'PENDING');
  const speed =
    position.speedMps === null
      ? ''
      : position.speedMps === 0
        ? loc.still
        : loc.speed(Math.round(position.speedMps * 3.6));
  return {
    title: loc.title(driverName(route), route.vehicle.name, formatTime(position.recordedAt, timeZone), ago),
    detail: [
      position.accuracyMeters === null ? '' : loc.accuracy(Math.round(position.accuracyMeters)),
      speed,
      next ? loc.next(stopTitle(next)) : '',
    ]
      .filter(Boolean)
      .join(' · '),
  };
}

/**
 * Prototype "Theo dõi tiến độ" (SCREENS §1.1): counts by result, one block per route with the server's route status
 * (`routes[].status`, T45: never worked out here), a bar split by result, the reason of a failed stop, the map coloured
 * by result. The page reads the plan again every 30 s while it is being delivered (planApi), and where the drivers
 * are (T32): an arrow per driver on the map, a line per route. A cancelled plan shows the same view with a notice.
 */
export function TrackView({ plan, updatedAt }: { plan: PlanDetail; updatedAt: number }) {
  const [selectedId, setSelectedId] = useState<number | string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const live = plan.status === 'PUBLISHED' || plan.status === 'IN_PROGRESS';
  const locationsQuery = useDriverLocations(plan.id, live);
  // Unknown until read: no line rather than "no position" while loading or when the request failed.
  const locations = live ? (locationsQuery.data ?? null) : null;
  // "4 phút trước" counts from when the positions were read, like the page's "cập nhật lúc".
  const now = locationsQuery.dataUpdatedAt;
  const locationOf = (route: PlanRoute) => locations?.find((found) => found.routeId === route.id);
  const drivers: MapDriver[] = plan.routes.flatMap((route, index) => {
    const position = locationOf(route)?.location;
    if (!position || route.status === 'COMPLETED') return [];
    return [
      {
        id: driverKey(route.id),
        routeIndex: index,
        lat: position.lat,
        lon: position.lon,
        headingDegrees: position.headingDegrees,
        label: loc.marker(driverName(route), route.vehicle.name, minutesAgo(position.recordedAt, now)),
      },
    ];
  });
  const routed = plan.routes.flatMap((route) => route.stops);
  const total = countByResult(routed);
  const infeasible = plan.unassignedStops.filter((stop) => stop.dropReason === 'INFEASIBLE');
  const date = formatDateLong(plan.planDate);
  const headline = live
    ? t.live(date, formatTime(new Date(updatedAt).toISOString(), plan.timezone))
    : plan.status === 'CANCELLED'
      ? date
      : t.ended(date);
  const { routes, unassigned } = mapData(plan, true);
  const selection = describeSelection(plan, allStops(plan).find((stop) => stop.id === selectedId) ?? null);
  const selected = allStops(plan).find((stop) => stop.id === selectedId);
  const selectedRoute = plan.routes.find((route) => driverKey(route.id) === selectedId);
  const selectedLocation = selectedRoute ? locationOf(selectedRoute) : undefined;
  const driverSelection =
    selectedRoute && selectedLocation ? describeDriver(selectedRoute, selectedLocation, plan.timezone, now) : null;

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
          </div>
          <p className="pw-muted">{headline}</p>
        </div>
        <div className="pw-plan-actions">
          {plan.status === 'PUBLISHED' ? (
            <Button variant="quiet" onClick={() => setCancelling(true)}>
              {vi.result.cancel}
            </Button>
          ) : null}
          {plan.status === 'CANCELLED' ? (
            <Link className={buttonClass('secondary')} to="/plans">
              {vi.result.backToList}
            </Link>
          ) : null}
        </div>
      </div>

      {plan.status === 'CANCELLED' ? (
        <section role="status" className="pw-cancelled">
          <strong>{vi.result.cancelledTitle}</strong>
          <span className="pw-muted">{vi.result.cancelledLead}</span>
        </section>
      ) : null}

      <section className="pw-stats" aria-label={t.results}>
        <div className="pw-stat">
          <StatusPill kind="stop" status="DELIVERED" />
          <span className="pw-stat-value">{t.delivered(total.DELIVERED, routed.length)}</span>
        </div>
        <div className="pw-stat">
          <StatusPill kind="stop" status="FAILED" />
          <span className="pw-stat-value">{t.points(total.FAILED)}</span>
        </div>
        <div className="pw-stat">
          <StatusPill kind="stop" status="SKIPPED" />
          <span className="pw-stat-value">{t.points(total.SKIPPED)}</span>
        </div>
        {infeasible.length > 0 ? (
          <div className="pw-stat">
            <StatusPill kind="dropReason" status="INFEASIBLE" />
            <span className="pw-stat-value">
              {t.infeasible(infeasible.length, infeasible.map((stop) => stop.label).join(', '))}
            </span>
          </div>
        ) : null}
      </section>

      <div className="pw-result-layout">
        <div className="pw-result-routes">
          {plan.routes.map((route, index) => (
            <TrackRoute
              key={route.id}
              route={route}
              index={index}
              timeZone={plan.timezone}
              selectedId={selectedId}
              onSelect={setSelectedId}
              location={locations ? locationLine(route, locationOf(route), plan.timezone, now) : null}
            />
          ))}
        </div>
        <section className="pw-result-map" aria-label={t.mapLabel}>
          <RouteMap
            depot={{ lat: plan.depot.lat, lon: plan.depot.lon }}
            routes={routes}
            unassigned={unassigned}
            drivers={drivers}
            selectedId={selectedId}
            onSelect={(id) => setSelectedId(typeof id === 'string' && id.startsWith('driver-') ? id : Number(id))}
            labels={MAP_LABELS}
          />
          <div role="status" className="pw-map-selection">
            {driverSelection ? (
              <>
                <strong>{driverSelection.title}</strong>
                <span className="pw-muted">{driverSelection.detail}</span>
              </>
            ) : selection && selected ? (
              <>
                <strong>{selection.title}</strong>
                <span className="pw-muted">
                  {[
                    selection.detail,
                    t.part[selected.status],
                    selected.failureReason ? t.reason(selected.failureReason) : '',
                  ]
                    .filter(Boolean)
                    .join(' · ')}
                </span>
              </>
            ) : (
              <span className="pw-muted">{t.pickHint}</span>
            )}
          </div>
          <ul className="pw-result-legend" aria-label={vi.map.legend}>
            <li>
              <span className="pw-dot pw-seg-ok" aria-hidden="true" />
              {t.part.DELIVERED}
            </li>
            <li>
              <span className="pw-dot pw-seg-fail" aria-hidden="true" />
              {t.part.FAILED}
            </li>
            <li>
              <span className="pw-dot pw-seg-skip" aria-hidden="true" />
              {t.part.SKIPPED}
            </li>
            {live ? (
              <li>
                <DriverIcon />
                {loc.legend}
              </li>
            ) : null}
          </ul>
        </section>
      </div>

      <CancelDialog
        plan={plan}
        open={cancelling}
        onClose={() => setCancelling(false)}
        onCancelled={() => setCancelling(false)}
      />
    </section>
  );
}

function TrackRoute({
  route,
  index,
  timeZone,
  selectedId,
  onSelect,
  location,
}: {
  route: PlanRoute;
  index: number;
  timeZone: string;
  selectedId: number | string | null;
  onSelect: (id: number) => void;
  location: { located: boolean; text: string } | null;
}) {
  const count = countByResult(route.stops);
  const parts = OUTCOMES.filter((outcome) => outcome === 'DELIVERED' || count[outcome] > 0).map((outcome) =>
    outcome === 'DELIVERED'
      ? `${count.DELIVERED}/${route.stops.length} ${t.part.DELIVERED}`
      : `${count[outcome]} ${t.part[outcome]}`,
  );
  const segments: Array<[Outcome, string]> = [
    ['DELIVERED', 'pw-seg-ok'],
    ['FAILED', 'pw-seg-fail'],
    ['SKIPPED', 'pw-seg-skip'],
    ['PENDING', 'pw-seg-wait'],
  ];
  return (
    <section className="pw-route-block" aria-labelledby={`track-${route.id}`}>
      <div className="pw-route-block-head pw-track-head">
        <div className="pw-track-title">
          <RouteBadge index={index} />
          <h2 id={`track-${route.id}`}>{driverName(route)}</h2>
          <span className="pw-muted">{vehicleText(route)}</span>
          <span className="pw-grow" />
          <StatusPill kind="route" status={route.status} />
        </div>
        <div className="pw-track-bar" role="img" aria-label={parts.join(', ')}>
          {segments.map(([outcome, className]) =>
            count[outcome] > 0 ? (
              <span key={outcome} className={className} style={{ flexGrow: count[outcome] }} />
            ) : null,
          )}
        </div>
        <p className="pw-muted pw-tabular">{parts.join(' · ')}</p>
        {location ? (
          <p className={location.located ? 'pw-track-loc' : 'pw-track-loc pw-track-loc-none'}>
            {location.located ? <DriverArrow index={index} /> : null}
            {location.text}
          </p>
        ) : null}
      </div>
      <div className="pw-table-wrap pw-route-table">
        <table className="pw-table">
          <caption className="pw-visually-hidden">{t.caption(driverName(route))}</caption>
          <thead>
            <tr>
              <th scope="col" className="pw-num pw-col-seq">
                {vi.result.sequence}
              </th>
              <th scope="col">{vi.result.order}</th>
              <th scope="col">{vi.result.address}</th>
              <th scope="col" className="pw-num">
                {vi.result.eta}
              </th>
              <th scope="col">{t.result}</th>
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
                </td>
                <td>{stop.address ?? <span className="pw-muted">{stopPlace(stop)}</span>}</td>
                <td className="pw-num">{stop.plannedArrival ? formatTime(stop.plannedArrival, timeZone) : '—'}</td>
                <td>
                  <StatusPill kind="stop" status={stop.status} />
                  {stop.status === 'FAILED' && stop.failureReason ? (
                    <div className="pw-track-reason">{t.reason(stop.failureReason)}</div>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

/** The map's driver marker, small, in the route's colour (prototype route block). */
function DriverArrow({ index }: { index: number }) {
  return (
    <svg viewBox="0 0 40 40" width="18" height="18" aria-hidden="true" focusable="false">
      <circle className={`pw-fill-r${routeColour(index)}`} cx="20" cy="20" r="18" stroke="#ffffff" strokeWidth="2.5" />
      <path d="M20 8.5L28.5 30L20 25L11.5 30Z" fill="#ffffff" />
    </svg>
  );
}
