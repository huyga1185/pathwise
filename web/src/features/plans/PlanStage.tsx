import { useEffect, useRef, useState, type ReactNode } from 'react';
import { formatPhone, formatTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { StatusPill } from '@/shared/ui';
import { ChevronDownIcon, ChevronLeftIcon, ChevronRightIcon } from '@/shared/ui/icons';
import { cx } from '@/shared/ui/cx';
import type { PlanDetail, PlanStop } from './planApi';
import { driverName, RouteBadge, stopPlace, stopTitle, vehicleText, windowText, type PlanRoute } from './planShared';

const r = vi.result;

/**
 * One route in the panel (G5.7): a group with the route's colour, driver · vehicle and its summary as a header that
 * folds it, then its stops as two-line rows (G5.8). `trailing` draws a stop's end of row (time or result).
 */
export function RouteGroup({
  route,
  index,
  summary,
  extra,
  stops,
  trailing,
  onOpen,
  title,
  tone,
  numbered = true,
}: {
  route?: PlanRoute;
  index?: number;
  summary: ReactNode;
  extra?: ReactNode;
  stops: PlanStop[];
  trailing: (stop: PlanStop) => ReactNode;
  onOpen: (stop: PlanStop) => void;
  /** A group that is not a route ("Chưa xếp tuyến"). */
  title?: string;
  tone?: 'signal';
  /** No sequence circle: stops not yet in a route's order (finding coordinates). */
  numbered?: boolean;
}) {
  const [open, setOpen] = useState(true);
  const heading = title ?? (route ? driverName(route) : '');
  const listId = `stops-${route?.id ?? 'unassigned'}`;
  return (
    <section className={cx('pw-stage-group', tone === 'signal' && 'is-signal')} aria-label={heading}>
      <button
        type="button"
        className="pw-stage-group-head"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen((value) => !value)}
      >
        {index !== undefined ? <RouteBadge index={index} /> : null}
        <span className="pw-stage-group-title">
          <span className="pw-stage-group-name">
            {heading}
            {route ? <span className="pw-muted"> · {vehicleText(route)}</span> : null}
          </span>
          <span className="pw-muted pw-tabular">{summary}</span>
        </span>
        <span className={cx('pw-stage-group-chevron', !open && 'is-closed')} aria-hidden="true">
          <ChevronDownIcon />
        </span>
      </button>
      {extra}
      {/* Folds with its height (a grid row from 0 to its content); folded, it is inert and hidden, so Tab and
          screen readers skip it (HIG Disclosure: the content shows and hides; reduced motion: no animation). */}
      <div className={cx('pw-stage-fold', !open && 'is-closed')} inert={!open}>
        <div className="pw-stage-fold-inner">
          <ul className={cx('pw-stage-rows', !numbered && 'is-plain')} id={listId}>
            {stops.map((stop) => (
              <li key={stop.id}>
                <button
                  type="button"
                  className="pw-stage-row"
                  data-stop={stop.id}
                  aria-label={r.stopOpen(stopTitle(stop))}
                  onClick={() => onOpen(stop)}
                >
                  {numbered ? (
                    <span className={cx('pw-stage-seq', index !== undefined && `pw-marker-r${(index % 7) + 1}`)}>
                      {stop.sequence ?? '–'}
                    </span>
                  ) : null}
                  <span className="pw-stage-row-main">
                    <span className="pw-stage-row-title">{stopTitle(stop)}</span>
                    <span className="pw-stage-row-sub">
                      {[stop.address ?? stopPlace(stop), windowText(stop)].filter(Boolean).join(' · ')}
                    </span>
                  </span>
                  <span className="pw-stage-row-end">{trailing(stop)}</span>
                  <span className="pw-stage-row-chevron" aria-hidden="true">
                    <ChevronRightIcon />
                  </span>
                </button>
              </li>
            ))}
          </ul>
        </div>
      </div>
    </section>
  );
}

/**
 * A stop's card (G5.9, G5-Q6; like a place card in Maps): the panel or sheet shows it instead of the list, with a
 * button back to the list named after the route. Focus goes to its title; back returns it to the stop's row.
 */
export function StopCard({
  plan,
  stop,
  onBack,
  children,
}: {
  plan: PlanDetail;
  stop: PlanStop;
  onBack: () => void;
  children?: ReactNode;
}) {
  const heading = useRef<HTMLHeadingElement>(null);
  useEffect(() => {
    heading.current?.focus({ preventScroll: true });
    // The panel / sheet may be scrolled down the list: show the card from its top.
    heading.current?.closest('section')?.scrollIntoView?.({ block: 'start' });
  }, [stop.id]);
  const route = plan.routes.find((item) => item.stops.some((candidate) => candidate.id === stop.id));
  const c = r.card;
  const rows: Array<[string, ReactNode]> = [
    [c.route, route ? c.routeValue(driverName(route), stop.sequence) : r.unassignedGroup],
    [c.recipient, stop.recipientName ?? '—'],
    [
      c.phone,
      stop.recipientPhone ? <a href={`tel:${stop.recipientPhone}`}>{formatPhone(stop.recipientPhone)}</a> : '—',
    ],
    [c.address, stop.address ?? stopPlace(stop)],
    [c.window, windowText(stop) || '—'],
    [c.eta, stop.plannedArrival ? formatTime(stop.plannedArrival, plan.timezone) : '—'],
  ];
  if (stop.status && plan.status !== 'DRAFT') {
    rows.push([c.result, <StatusPill key="result" kind="stop" status={stop.status} />]);
    if (stop.failureReason) rows.push([c.reason, stop.failureReason]);
  }
  if (stop.note) rows.push([c.note, stop.note]);
  return (
    <section className="pw-stop-card" aria-labelledby={`stop-card-${stop.id}`}>
      <button type="button" className="pw-back-link pw-stop-card-back" onClick={onBack}>
        <ChevronLeftIcon />
        <span>{route ? driverName(route) : r.unassignedGroup}</span>
      </button>
      <h2 id={`stop-card-${stop.id}`} ref={heading} tabIndex={-1} className="pw-stop-card-title">
        {stopTitle(stop)}
      </h2>
      {children}
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
