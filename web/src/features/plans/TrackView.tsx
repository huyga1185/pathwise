import { useEffect, useRef, useState, type ReactNode } from 'react';
import { DriverIcon, RouteMap, routeColour, type MapDriver } from '@/platform/map';
import { formatDateLong, formatPhone, formatTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { BackLink } from '@/shared/ui/BackLink';
import { MapStage, StatusPill, type RowMenuItem } from '@/shared/ui';
import { ChevronLeftIcon } from '@/shared/ui/icons';
import { useStoreReadOnly } from '@/data/meQuery';
import { CopySheet } from './CopySheet';
import { CancelAlert } from './PlanDialogs';
import { MoreMenu, RouteGroup, StopCard } from './PlanStage';
import { allStops, useDriverLocations, type DriverLocation, type PlanDetail, type PlanStop } from './planApi';
import { driverName, MAP_LABELS, mapData, stopTitle, vehicleText, type PlanRoute } from './planShared';

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

/**
 * Prototype "Theo dõi tiến độ" (SCREENS §1.1): counts by result, one block per route with the server's route status
 * (`routes[].status`, T45: never worked out here), a bar split by result, the reason of a failed stop, the map coloured
 * by result. The page reads the plan again every 30 s while it is being delivered (planApi), and where the drivers
 * are (T32): an arrow per driver on the map, a line per route. A cancelled plan shows the same view with a notice.
 */
export function TrackView({ plan, updatedAt }: { plan: PlanDetail; updatedAt: number }) {
  const [selectedId, setSelectedId] = useState<number | string | null>(null);
  const [cancelling, setCancelling] = useState(false);
  const [copying, setCopying] = useState(false);
  const { readOnly } = useStoreReadOnly();
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
  const date = formatDateLong(plan.planDate);
  const headline = live
    ? t.live(date, formatTime(new Date(updatedAt).toISOString(), plan.timezone))
    : plan.status === 'CANCELLED'
      ? date
      : t.ended(date);
  const { routes, unassigned } = mapData(plan, true);
  const selected = allStops(plan).find((stop) => stop.id === selectedId);
  const selectedRoute = plan.routes.find((route) => driverKey(route.id) === selectedId);
  const selectedLocation = selectedRoute ? locationOf(selectedRoute) : undefined;

  const moreItems: RowMenuItem[] = readOnly
    ? []
    : [
        { label: vi.planCopy.action, onSelect: () => setCopying(true) },
        ...(plan.status === 'PUBLISHED'
          ? [{ label: vi.result.cancel, destructive: true, onSelect: () => setCancelling(true) }]
          : []),
      ];
  const unplaced = plan.unassignedStops;
  const back = () => {
    const id = selectedId;
    setSelectedId(null);
    // Back to the list: focus returns to what opened the card (WCAG 2.4.3).
    window.requestAnimationFrame(() =>
      document
        .querySelector<HTMLElement>(typeof id === 'string' ? `[data-driver="${id}"]` : `[data-stop="${id}"]`)
        ?.focus(),
    );
  };

  return (
    <MapStage
      label={vi.result.panel}
      map={(inset) => (
        <RouteMap
          staged
          inset={inset}
          depot={{ lat: plan.depot.lat, lon: plan.depot.lon }}
          routes={routes}
          unassigned={unassigned}
          drivers={drivers}
          selectedId={selectedId}
          onSelect={(id) => setSelectedId(typeof id === 'string' && id.startsWith('driver-') ? id : Number(id))}
          labels={MAP_LABELS}
        />
      )}
    >
      <div className="pw-stage-head" data-peek>
        <BackLink to="/plans" label={vi.plans.title} />
        <div className="pw-plan-title">
          <h1 id="plan-title" className="pw-page-title pw-stage-title">
            {plan.name}
          </h1>
          <StatusPill kind="plan" status={plan.status} />
          {/* The only action here: at the trailing end of the title row (HIG Toolbars), not a row of its own. */}
          <span className="pw-stage-title-more">
            <MoreMenu items={moreItems} />
          </span>
        </div>
        <p className="pw-muted">{headline}</p>
        {/* The whole plan's progress: a bar by result and the same in words (G5-Q13; WCAG 1.4.1). */}
        <ResultBar stops={routed} label={t.planBar} />
        <p className="pw-stage-summary pw-tabular">{resultParts(routed).join(' · ')}</p>
        {/* What the map's colours mean (G5-Q15). */}
        <ul className="pw-stage-legend" aria-label={vi.map.legend}>
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
      </div>

      {copying ? <CopySheet planId={plan.id} planName={plan.name} open onClose={() => setCopying(false)} /> : null}

      {plan.status === 'CANCELLED' ? (
        <section role="status" className="pw-cancelled">
          <strong>{vi.result.cancelledTitle}</strong>
          <span className="pw-muted">{vi.result.cancelledLead}</span>
        </section>
      ) : null}

      {selectedRoute ? (
        <DriverCard route={selectedRoute} found={selectedLocation} timeZone={plan.timezone} now={now} onBack={back} />
      ) : selected ? (
        <StopCard plan={plan} stop={selected} onBack={back} />
      ) : (
        <div className="pw-stage-list">
          {plan.routes.map((route, index) => {
            const location = locations ? locationLine(route, locationOf(route), plan.timezone, now) : null;
            const located = Boolean(locationOf(route)?.location) && route.status !== 'COMPLETED';
            return (
              <RouteGroup
                key={route.id}
                route={route}
                index={index}
                summary={resultParts(route.stops).join(' · ')}
                extra={
                  <div className="pw-stage-group-extra">
                    <StatusPill kind="route" status={route.status} />
                    <ResultBar stops={route.stops} label={(parts) => parts} />
                    {location ? (
                      located ? (
                        <button
                          type="button"
                          className="pw-track-loc pw-link-button"
                          data-driver={driverKey(route.id)}
                          aria-label={t.driverOpen(driverName(route))}
                          onClick={() => setSelectedId(driverKey(route.id))}
                        >
                          <DriverArrow index={index} />
                          {location.text}
                        </button>
                      ) : (
                        <p className="pw-track-loc pw-track-loc-none">{location.text}</p>
                      )
                    ) : null}
                  </div>
                }
                stops={route.stops}
                trailing={(stop) => (
                  <>
                    <StatusPill kind="stop" status={stop.status} />
                    {stop.status === 'FAILED' && stop.failureReason ? (
                      <span className="pw-track-reason">{t.reason(stop.failureReason)}</span>
                    ) : null}
                  </>
                )}
                onOpen={(stop) => setSelectedId(stop.id)}
              />
            );
          })}
          {unplaced.length > 0 ? (
            <RouteGroup
              title={vi.result.unassignedGroup}
              tone="signal"
              summary={vi.result.unassignedLead}
              stops={unplaced}
              trailing={(stop) => <StatusPill kind="dropReason" status={stop.dropReason ?? 'INFEASIBLE'} />}
              onOpen={(stop) => setSelectedId(stop.id)}
            />
          ) : null}
        </div>
      )}

      <CancelAlert
        plan={plan}
        open={cancelling}
        onClose={() => setCancelling(false)}
        onCancelled={() => setCancelling(false)}
      />
    </MapStage>
  );
}

/** Results of some stops in words: "2/23 đã giao · 1 không giao được · 1 bỏ qua". */
function resultParts(stops: PlanStop[]): string[] {
  const count = countByResult(stops);
  return OUTCOMES.filter((outcome) => outcome === 'DELIVERED' || count[outcome] > 0).map((outcome) =>
    outcome === 'DELIVERED'
      ? `${count.DELIVERED}/${stops.length} ${t.part.DELIVERED}`
      : `${count[outcome]} ${t.part[outcome]}`,
  );
}

/** A bar split by result; its name says the same in words (the colours never stand alone). */
function ResultBar({ stops, label }: { stops: PlanStop[]; label: (parts: string) => string }) {
  const count = countByResult(stops);
  const segments: Array<[Outcome, string]> = [
    ['DELIVERED', 'pw-seg-ok'],
    ['FAILED', 'pw-seg-fail'],
    ['SKIPPED', 'pw-seg-skip'],
    ['PENDING', 'pw-seg-wait'],
  ];
  return (
    <div className="pw-track-bar" role="img" aria-label={label(resultParts(stops).join(', '))}>
      {segments.map(([outcome, className]) =>
        count[outcome] > 0 ? <span key={outcome} className={className} style={{ flexGrow: count[outcome] }} /> : null,
      )}
    </div>
  );
}

/**
 * A driver's card (G5-Q14), opened from their arrow on the map or the location line of their route: where and when,
 * how sure and how fast, the route's progress and the next stop, a call. Back returns to all routes.
 */
function DriverCard({
  route,
  found,
  timeZone,
  now,
  onBack,
}: {
  route: PlanRoute;
  found: DriverLocation | undefined;
  timeZone: string;
  now: number;
  onBack: () => void;
}) {
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    heading.current?.focus({ preventScroll: true });
  }, [route.id]);
  const c = t.card;
  const position = found?.location;
  const next = route.stops.find((stop) => stop.status === 'PENDING');
  const rows: Array<[string, ReactNode]> = [
    [c.vehicle, vehicleText(route)],
    [
      c.location,
      position
        ? loc.updated(minutesAgo(position.recordedAt, now), formatTime(position.recordedAt, timeZone))
        : (locationLine(route, found, timeZone, now)?.text ?? '—'),
    ],
  ];
  if (position?.accuracyMeters != null) rows.push([c.accuracy, `${Math.round(position.accuracyMeters)} m`]);
  if (position?.speedMps != null)
    rows.push([c.speed, position.speedMps === 0 ? loc.still : loc.speed(Math.round(position.speedMps * 3.6))]);
  rows.push([c.progress, resultParts(route.stops).join(' · ')]);
  if (next) rows.push([c.next, stopTitle(next)]);
  if (route.driver.phoneNumber)
    rows.push([
      c.phone,
      <a key="phone" href={`tel:${route.driver.phoneNumber}`}>
        {formatPhone(route.driver.phoneNumber)}
      </a>,
    ]);
  return (
    <section className="pw-stop-card" aria-labelledby={`driver-card-${route.id}`}>
      <button type="button" className="pw-back-link pw-stop-card-back" onClick={onBack}>
        <ChevronLeftIcon />
        <span>{t.allRoutes}</span>
      </button>
      <h2 id={`driver-card-${route.id}`} ref={heading} tabIndex={-1} className="pw-stop-card-title">
        {driverName(route)}
      </h2>
      <dl className="pw-stage-group pw-stop-card-facts">
        {rows.map(([term, value]) => (
          <div key={term}>
            <dt>{term}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
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
