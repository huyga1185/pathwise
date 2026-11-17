import { useStoreReadOnly } from '@/data/meQuery';
import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type MouseEvent,
  type PointerEvent,
  type RefObject,
} from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { useStoreQuery } from '@/data/storeQuery';
import { messageFor } from '@/shared/errors';
import { formatDateShort, formatDayTime, formatDistance, formatDuration, todayIn } from '@/shared/format';
import { reducedMotion, useExit } from '@/shared/hooks/useExit';
import { cx } from '@/shared/ui/cx';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  Button,
  buttonClass,
  ChevronLeftIcon,
  ChevronRightIcon,
  CopyIcon,
  DataTable,
  DatePickerFilter,
  ErrorBanner,
  PageJump,
  PlaceholderRows,
  PlusIcon,
  RouteIcon,
  PopUpFilter,
  StatusPill,
  type DataTableColumn,
  type TableSort,
} from '@/shared/ui';
import { PAGE_SIZE, usePlans, type PlanStatus, type PlanSummary } from './planApi';
import { CopyDialog } from './CopyDialog';
import './plans.css';

const STATUSES = new Set<string>(vi.plans.statusOptions.map((option) => option.value));
const DEVICE_TIME_ZONE = Intl.DateTimeFormat().resolvedOptions().timeZone;

/** "2026-12-01" → "01/12/2026". */
function shortDate(localDate: string): string {
  const [year, month, day] = localDate.split('-');
  return `${day}/${month}/${year}`;
}

/** URL ?order= ↔ the API's sort; none = plan date, newest first (the API's default). */
const ORDERS: Record<string, { sort: TableSort; api: string }> = {
  'date-asc': { sort: { key: 'date', direction: 'asc' }, api: 'planDate,asc' },
  'name-asc': { sort: { key: 'name', direction: 'asc' }, api: 'name,asc' },
  'name-desc': { sort: { key: 'name', direction: 'desc' }, api: 'name,desc' },
};
const DEFAULT_SORT: TableSort = { key: 'date', direction: 'desc' };

/**
 * "Danh sách kế hoạch" (SCREENS §1.1, OWNER_UI_REDESIGN G2): filters by date and status, the order and the page on the
 * URL (`?date=&status=&order=&page=`). Pages with a clear end (the owner's choice, G2.9): on a wide window a page is as
 * many rows as fit the window (HIG Scroll views: a page "typically the current height… of the view"; Lists and tables:
 * "limit the number of rows"); on a phone 20 rows that scroll. A short title with the main action at its end (HIG Toolbars); the
 * filters as two compact buttons right above the list (HIG Pickers, Pop-up buttons), "Xoá bộ lọc" only while
 * filtering; a table with sortable headings on a wide window, a list of rows on a phone (HIG Lists and tables);
 * placeholder rows while loading (HIG Loading); an empty state like ContentUnavailableView. On a phone a row keeps
 * only what fits (name, date and stops, status, chevron); copying is in the plan's own page.
 */
export function PlanListPage() {
  const [params, setParams] = useSearchParams();
  const { readOnly } = useStoreReadOnly();
  const [copying, setCopying] = useState<PlanSummary | null>(null);
  const navigate = useNavigate();
  const wide = useMediaQuery(WIDE_QUERY);
  const date = /^\d{4}-\d{2}-\d{2}$/.test(params.get('date') ?? '') ? (params.get('date') as string) : undefined;
  const rawStatus = params.get('status') ?? '';
  const status = STATUSES.has(rawStatus) ? (rawStatus as PlanStatus) : undefined;
  const order = ORDERS[params.get('order') ?? ''];
  const sort = order?.sort ?? DEFAULT_SORT;
  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0);
  const result = useRef<HTMLDivElement>(null);
  const probe = useRef<HTMLDivElement>(null);
  const filters = useRef<HTMLDivElement>(null);
  const fit = useFitRows(wide, result, probe, filters);
  const size = wide ? (fit?.rows ?? null) : PAGE_SIZE;
  const plans = usePlans({ date, status, sort: order?.api, page, size: size ?? PAGE_SIZE }, size !== null);
  const store = useStoreQuery();
  const timeZone = store.data?.timezone ?? DEVICE_TIME_ZONE;

  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)));
    if (!('page' in changes)) next.delete('page'); // another filter or order starts again at page 1
    setParams(next, { replace: true });
  };
  const goToPage = (target: number) => {
    update({ page: target > 0 ? String(target) : undefined });
    // On a phone the next page starts at the top of the list: scroll back only as far as needed to see its start
    // (HIG Scroll views: "scroll automatically to help people find their place… only as much as necessary").
    const top = result.current?.getBoundingClientRect().top ?? 0;
    if (!wide && top < 0) {
      result.current?.scrollIntoView({ block: 'start', behavior: reducedMotion() ? 'auto' : 'smooth' });
    }
  };

  // A wider or narrower window changes the rows a page holds: keep the first plan shown on the page shown.
  const [lastSize, setLastSize] = useState(size);
  if (size !== lastSize) {
    setLastSize(size);
    if (size && lastSize && page > 0) {
      const next = new URLSearchParams(params);
      const moved = Math.floor((page * lastSize) / size);
      if (moved > 0) next.set('page', String(moved));
      else next.delete('page');
      setParams(next, { replace: true });
    }
  }
  // A heading sorts by its column; clicking the sorted one again reverses it (HIG Lists and tables).
  const onSort = (key: string) => {
    const direction = sort.key === key ? (sort.direction === 'asc' ? 'desc' : 'asc') : key === 'date' ? 'desc' : 'asc';
    const next = `${key}-${direction}`;
    update({ order: next === 'date-desc' ? undefined : next });
  };

  // Phone: touch and hold a row for its context menu ("Sao chép…"), like Mail and Notes; the plan's own page has the
  // same command (HIG Context menus: "Always make context menu items available in the main interface, too").
  const [menuFor, setMenuFor] = useState<{ row: PlanSummary; rect: DOMRect } | null>(null);
  const held = useRef(false);
  const hold = useRef<{ timer: number; x: number; y: number } | null>(null);
  const stopHold = () => {
    if (hold.current) window.clearTimeout(hold.current.timer);
    hold.current = null;
  };
  const holdHandlers = (row: PlanSummary) => ({
    onPointerDown: (event: PointerEvent<HTMLLIElement>) => {
      if (event.pointerType !== 'touch') return;
      const element = event.currentTarget;
      held.current = false;
      stopHold();
      hold.current = {
        x: event.clientX,
        y: event.clientY,
        timer: window.setTimeout(() => {
          held.current = true;
          hold.current = null;
          setMenuFor({ row, rect: element.getBoundingClientRect() });
        }, 500),
      };
    },
    onPointerMove: (event: PointerEvent<HTMLLIElement>) => {
      const start = hold.current;
      if (start && Math.hypot(event.clientX - start.x, event.clientY - start.y) > 10) stopHold();
    },
    onPointerUp: stopHold,
    onPointerCancel: stopHold,
    // Android's long press and a right click: the same menu instead of the browser's.
    onContextMenu: (event: MouseEvent<HTMLLIElement>) => {
      event.preventDefault();
      stopHold();
      // A right click opens it at the pointer; Android's long press, under the row like touch and hold.
      const touch = (event.nativeEvent as globalThis.PointerEvent).pointerType === 'touch';
      setMenuFor({
        row,
        rect: touch
          ? event.currentTarget.getBoundingClientRect()
          : new DOMRect(event.clientX - 8, event.clientY - 8, 0, 0),
      });
    },
  });

  const copyButton = (row: PlanSummary) =>
    readOnly ? null : (
      <button
        type="button"
        className="pw-icon-button pw-plans-copy"
        aria-label={vi.planCopy.actionOf(row.name)}
        title={vi.planCopy.action}
        onClick={(event) => {
          event.stopPropagation();
          setCopying(row);
        }}
      >
        <CopyIcon />
      </button>
    );

  const columns: DataTableColumn<PlanSummary>[] = [
    {
      key: 'name',
      header: vi.plans.name,
      sortable: true,
      className: 'pw-plans-col-name',
      // At most two lines (up to 150 characters); the whole name shows on hover and in the plan's page (HIG
      // Typography: truncate in a scrolling list only when people can open a view with the rest).
      render: (row) => (
        <>
          <Link
            to={`/plans/${row.id}`}
            className="pw-plans-link"
            title={row.name}
            onClick={(event) => event.stopPropagation()}
          >
            {row.name}
          </Link>
          <HiddenColumns row={row} timeZone={timeZone} />
        </>
      ),
    },
    {
      key: 'date',
      header: vi.plans.date,
      sortable: true,
      render: (row) => <span className="pw-tabular pw-nowrap">{formatDateShort(row.planDate)}</span>,
    },
    { key: 'status', header: vi.plans.status, render: (row) => <StatusPill kind="plan" status={row.status} /> },
    { key: 'stops', header: vi.plans.stops, numeric: true, render: (row) => row.stopCount },
    {
      key: 'routes',
      header: vi.plans.routes,
      numeric: true,
      className: 'pw-plans-col-more',
      render: (row) => row.routeCount,
    },
    {
      key: 'distance',
      header: vi.plans.distance,
      numeric: true,
      className: 'pw-plans-col-more',
      render: (row) => formatDistance(row.totalDistanceMeters),
    },
    {
      key: 'duration',
      header: vi.plans.duration,
      numeric: true,
      className: 'pw-plans-col-extra',
      render: (row) => formatDuration(row.totalDurationSeconds),
    },
    {
      key: 'optimized',
      header: vi.plans.optimizedAt,
      numeric: true,
      className: 'pw-plans-col-extra',
      render: (row) => (row.optimizedAt ? formatDayTime(row.optimizedAt, timeZone) : '—'),
    },
    ...(readOnly
      ? []
      : [
          {
            key: 'actions',
            header: <span className="pw-visually-hidden">{vi.plans.actions}</span>,
            align: 'end' as const,
            render: copyButton,
          },
        ]),
  ];

  const data = plans.data;
  const items = data?.items ?? [];
  const total = data?.totalElements ?? 0;
  const totalPages = data?.totalPages ?? 0;
  const filtered = Boolean(date || status);
  const loading = data === undefined && !plans.isError;
  const empty = data !== undefined && total === 0;
  // A page past the end (an old link, plans removed): show the last page.
  const pastEnd = data !== undefined && !plans.isPlaceholderData && totalPages > 0 && page >= totalPages;
  useEffect(() => {
    if (!pastEnd) return;
    const next = new URLSearchParams(params);
    if (totalPages > 1) next.set('page', String(totalPages - 1));
    else next.delete('page');
    setParams(next, { replace: true });
  }, [pastEnd, params, setParams, totalPages]);

  // What the list shows (placeholders, rows or the empty state) for which filters and order. While another filter's
  // plans load, the old result stays, dimmed; when they arrive, the new result settles in (OWNER_UI_REDESIGN §7,
  // G2.10). Not on the first render: the page itself just moved in.
  // Another page slides in from the side it comes from (next from the right, previous from the left), like turning a
  // page (HIG Scroll views: page-by-page); a new filter or order settles in from below.
  const filterKey = [loading ? 'loading' : empty ? 'empty' : 'list', date, status, params.get('order')].join('|');
  const resultKey = `${filterKey}|${page}`;
  const [shown, setShown] = useState({ key: resultKey, filterKey, page, motion: '' });
  if (!plans.isPlaceholderData && resultKey !== shown.key) {
    const motion = filterKey !== shown.filterKey ? 'is-changed' : page > shown.page ? 'is-next' : 'is-prev';
    setShown({ key: resultKey, filterKey, page, motion });
  }
  const clearPresence = useExit(filtered, 140);

  return (
    <section className="pw-page-section" aria-labelledby="plans-title">
      <div className="pw-list-head">
        <h1 id="plans-title" className="pw-page-title">
          {vi.plans.title}
        </h1>
        {!readOnly ? (
          <Link
            className={wide ? buttonClass('primary') : 'pw-icon-button pw-list-add'}
            to="/plans/new"
            aria-label={wide ? undefined : vi.plans.create}
            title={wide ? undefined : vi.plans.create}
          >
            <PlusIcon />
            {wide ? vi.plans.create : null}
          </Link>
        ) : null}
      </div>

      <div ref={filters} className="pw-list-filters" role="group" aria-label={vi.plans.filterLabel}>
        <DatePickerFilter
          label={vi.plans.date}
          value={date}
          emptyText={vi.plans.allDates}
          today={todayIn(timeZone)}
          clearText={vi.plans.anyDate}
          previousMonthText={vi.plans.previousMonth}
          nextMonthText={vi.plans.nextMonth}
          onChange={(value) => update({ date: value })}
        />
        <PopUpFilter
          label={vi.plans.status}
          value={status}
          emptyText={vi.plans.allStatuses}
          options={vi.plans.statusOptions}
          onChange={(value) => update({ status: value })}
        />
        {clearPresence !== 'closed' ? (
          <ClearFilters
            closing={clearPresence === 'closing'}
            onClear={() => update({ date: undefined, status: undefined })}
          />
        ) : null}
      </div>

      {plans.isError ? <ErrorBanner title={messageFor(plans.error)} /> : null}

      <div ref={probe} className="pw-fit-probe" aria-hidden="true" inert>
        {wide ? (
          <>
            <div className="pw-list-group pw-table-wrap pw-plans-table">
              <table className="pw-table">
                <thead>
                  <tr>
                    <th>{vi.plans.name}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="pw-plans-col-name">
                      <span className="pw-plans-link">
                        {vi.plans.name}
                        <br />
                        {vi.plans.name}
                      </span>
                      <span className="pw-plans-more">
                        <span className="pw-plans-more-routes">{vi.plans.routes}</span>
                        <span className="pw-plans-more-distance">{vi.plans.distance}</span>
                        <span className="pw-plans-more-duration">{vi.plans.duration}</span>
                        <span className="pw-plans-more-optimized">{vi.plans.optimizedAt}</span>
                      </span>
                    </td>
                    <td>
                      <button type="button" className="pw-icon-button" tabIndex={-1}>
                        <CopyIcon />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <Pager page={0} totalPages={2} onPage={() => {}} />
          </>
        ) : null}
      </div>
      <div
        ref={result}
        key={shown.key}
        className={cx('pw-list-result', shown.motion, plans.isPlaceholderData && 'is-stale')}
        // A wide window keeps a full page's height, so the pager stays put on a short last page (HIG Page controls:
        // "To ensure people always know where to find a page control… position it near the bottom of the view").
        style={wide && fit && !empty ? { minHeight: fit.height } : undefined}
        aria-busy={plans.isPlaceholderData || undefined}
      >
        {loading ? (
          <div className="pw-list-group">
            <PlaceholderRows rows={Math.min(size ?? 6, 12)} label={vi.plans.loading} />
          </div>
        ) : empty ? (
          <section className="pw-list-empty" aria-labelledby="empty-title">
            <span className="pw-list-empty-symbol" aria-hidden="true">
              <RouteIcon />
            </span>
            <h2 id="empty-title">
              {date ? vi.plans.emptyTitleOn(shortDate(date)) : status ? vi.plans.emptyFiltered : vi.plans.emptyTitle}
            </h2>
            <p>{vi.plans.emptyLead}</p>
            {!readOnly ? (
              <div className="pw-list-empty-actions">
                <Link className={buttonClass('primary', 'lg')} to="/plans/new">
                  {vi.plans.create}
                </Link>
              </div>
            ) : null}
          </section>
        ) : (
          <section className="pw-plans-list" aria-label={vi.plans.listLabel}>
            {wide ? (
              <DataTable
                caption={vi.plans.caption}
                columns={columns}
                rows={items}
                onRowClick={(row) => navigate(`/plans/${row.id}`)}
                className="pw-list-group pw-plans-table"
                sort={sort}
                onSort={onSort}
                onRowContextMenu={
                  readOnly
                    ? undefined
                    : (row, point) => setMenuFor({ row, rect: new DOMRect(point.x - 8, point.y - 8, 0, 0) })
                }
              />
            ) : (
              <ul className="pw-list-group pw-list-rows" aria-label={vi.plans.caption}>
                {items.map((row) => (
                  <li
                    key={row.id}
                    className={cx('pw-list-row', menuFor?.row.id === row.id && 'is-held')}
                    {...(readOnly ? {} : holdHandlers(row))}
                  >
                    <Link
                      to={`/plans/${row.id}`}
                      className="pw-list-row-main"
                      onClick={(event) => {
                        // The touch-and-hold that opened the menu is not also a tap on the row.
                        if (held.current) {
                          event.preventDefault();
                          held.current = false;
                        }
                      }}
                    >
                      <span className="pw-list-row-title">{row.name}</span>
                      <span className="pw-list-row-sub">
                        {formatDateShort(row.planDate)} · {vi.plans.stopCount(row.stopCount)}
                      </span>
                    </Link>
                    <StatusPill kind="plan" status={row.status} />
                    <span className="pw-list-row-chevron" aria-hidden="true">
                      <ChevronRightIcon />
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}
      </div>
      {/* Outside the result, which is drawn again on each page: the pager keeps its focus while pages turn. */}
      {data !== undefined && !empty ? <Pager page={page} totalPages={totalPages} onPage={goToPage} /> : null}
      {menuFor ? (
        <RowMenu
          rect={menuFor.rect}
          label={menuFor.row.name}
          onClose={() => setMenuFor(null)}
          items={[{ label: vi.planCopy.menuItem, onSelect: () => setCopying(menuFor.row) }]}
        />
      ) : null}
      {copying ? (
        <CopyDialog planId={copying.id} planName={copying.name} open onClose={() => setCopying(null)} />
      ) : null}
    </section>
  );
}

/**
 * Pages of a list with a clear end: "‹ Trang 4/12 ›", centred under the list (HIG Page controls: "Center a page control
 * at the bottom of the view"). The arrows go one page back or on, round grey buttons (HIG Buttons), dimmed with
 * aria-disabled at either end so a focused arrow keeps its focus. "Trang 4/12" opens a small panel: first page, a
 * field for the page number, last page (PageJump). One page: no pager.
 */
function Pager({ page, totalPages, onPage }: { page: number; totalPages: number; onPage: (page: number) => void }) {
  if (totalPages <= 1) return null;
  const current = Math.min(page, totalPages - 1);
  const last = totalPages - 1;
  return (
    <nav className="pw-pager" aria-label={vi.plans.pagerLabel}>
      <button
        type="button"
        className="pw-pager-step"
        aria-label={vi.plans.previous}
        title={vi.plans.previous}
        aria-disabled={current === 0}
        onClick={() => current > 0 && onPage(current - 1)}
      >
        <ChevronLeftIcon />
      </button>
      <PageJump
        page={current}
        pages={totalPages}
        buttonText={vi.plans.pageOf(current + 1, totalPages)}
        buttonLabel={vi.plans.pageJump(current + 1, totalPages)}
        fieldLabel={vi.plans.pageField}
        firstLabel={vi.plans.firstPage}
        lastLabel={vi.plans.lastPage}
        onPage={onPage}
      />
      <button
        type="button"
        className="pw-pager-step"
        aria-label={vi.plans.next}
        title={vi.plans.next}
        aria-disabled={current === last}
        onClick={() => current < last && onPage(current + 1)}
      >
        <ChevronRightIcon />
      </button>
    </nav>
  );
}

/**
 * Rows of the table that fit the window under the list's top with the pager below, nothing to scroll; measured again
 * when the window, the filters' row or the text size changes (rows grow with larger text, so fewer fit — HIG Layout).
 * At least 3 (a very short window scrolls), at most 100 (API_SPEC §1). `null` until measured; 20 where nothing can
 * be measured (tests). Also the height of a full page, which the list keeps on a short last page.
 */
function useFitRows(
  enabled: boolean,
  result: RefObject<HTMLDivElement | null>,
  probe: RefObject<HTMLDivElement | null>,
  filters: RefObject<HTMLDivElement | null>,
): { rows: number; height: number } | null {
  const [fit, setFit] = useState<{ rows: number; height: number } | null>(null);
  useLayoutEffect(() => {
    if (!enabled) return undefined;
    const measure = () => {
      const list = result.current;
      const sample = probe.current;
      if (!list || !sample) return;
      const head = sample.querySelector('thead')?.getBoundingClientRect().height ?? 0;
      const row = sample.querySelector('tbody tr')?.getBoundingClientRect().height ?? 0;
      const pager = sample.querySelector('.pw-pager')?.getBoundingClientRect().height ?? 0;
      if (row <= 0) {
        setFit({ rows: PAGE_SIZE, height: 0 });
        return;
      }
      const main = list.closest('main');
      const bottom = main ? Number.parseFloat(getComputedStyle(main).paddingBottom) || 0 : 0;
      const top = list.getBoundingClientRect().top + window.scrollY;
      const gap = Number.parseFloat(getComputedStyle(list.parentElement ?? list).rowGap) || 0; // table ↔ pager
      const room = window.innerHeight - top - head - gap - pager - bottom - 2;
      // Whole pixels: a row measures 56.5 here and 57 in the table (borders), which adds up over a page.
      const rows = Math.max(3, Math.min(100, Math.floor(room / Math.ceil(row))));
      const height = Math.ceil(head + rows * Math.ceil(row) + 2);
      setFit((was) => (was && was.rows === rows && was.height === height ? was : { rows, height }));
    };
    measure();
    window.addEventListener('resize', measure);
    const observer = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(measure);
    if (probe.current) observer?.observe(probe.current);
    if (filters.current) observer?.observe(filters.current);
    return () => {
      window.removeEventListener('resize', measure);
      observer?.disconnect();
    };
  }, [enabled, result, probe, filters]);
  return enabled ? fit : null;
}

/**
 * The values of the columns a narrow table leaves out, as a secondary line under the name ("3 tuyến · 15,4 km ·
 * 5 giờ 06 phút"), so no data disappears (HIG Lists and tables: a narrow table keeps content "recognizable and easy
 * to read"; a row style "that fits the information you need to display"). Each part shows only while its column is
 * hidden (plans.css), and only when it has a value.
 */
function HiddenColumns({ row, timeZone }: { row: PlanSummary; timeZone: string }) {
  const parts: Array<[string, string | null]> = [
    ['pw-plans-more-routes', row.routeCount > 0 ? vi.plans.routeCount(row.routeCount) : null],
    ['pw-plans-more-distance', row.totalDistanceMeters !== null ? formatDistance(row.totalDistanceMeters) : null],
    ['pw-plans-more-duration', row.totalDurationSeconds !== null ? formatDuration(row.totalDurationSeconds) : null],
    [
      'pw-plans-more-optimized',
      row.optimizedAt ? vi.plans.optimizedOn(formatDayTime(row.optimizedAt, timeZone)) : null,
    ],
  ];
  const shown = parts.filter((part): part is [string, string] => part[1] !== null);
  if (shown.length === 0) return null;
  return (
    <span className="pw-plans-more">
      {shown.map(([className, text]) => (
        <span key={className} className={className}>
          {text}
        </span>
      ))}
    </span>
  );
}

/**
 * "Xoá bộ lọc": pops in beside the filters and back out where it was. While it leaves, the filters re-flow (the date
 * button gets shorter), so it is lifted out of the row at its last place instead of being carried along; it is inert,
 * not disabled, so it does not dim before fading (the disabled style). When a filter changes and the row re-flows
 * while it stays, it glides to its new place (another row too) instead of jumping.
 */
function ClearFilters({ closing, onClear }: { closing: boolean; onClear: () => void }) {
  const button = useRef<HTMLButtonElement>(null);
  const place = useRef<{ left: number; top: number } | null>(null);
  // Every render: remember where it sits while shown; once it leaves, pin it there.
  useLayoutEffect(() => {
    const element = button.current;
    if (!element) return;
    if (!closing) {
      const was = place.current;
      place.current = { left: element.offsetLeft, top: element.offsetTop };
      // The filters re-flowed and it moved (another row, another place): glide there instead of jumping.
      const dx = was ? was.left - place.current.left : 0;
      const dy = was ? was.top - place.current.top : 0;
      if ((dx || dy) && !reducedMotion()) {
        element.animate([{ transform: `translate(${dx}px, ${dy}px)` }, { transform: 'none' }], {
          duration: 280,
          easing: 'cubic-bezier(0.2, 0.8, 0.2, 1)',
        });
      }
      element.style.removeProperty('position');
      element.style.removeProperty('left');
      element.style.removeProperty('top');
    } else if (place.current) {
      element.style.position = 'absolute';
      element.style.left = `${place.current.left}px`;
      element.style.top = `${place.current.top}px`;
    }
  });
  return (
    <Button
      ref={button}
      variant="quiet"
      className={cx('pw-list-clear', closing && 'is-closing')}
      inert={closing}
      aria-hidden={closing || undefined}
      onClick={onClear}
    >
      {vi.plans.clearFilters}
    </Button>
  );
}

/**
 * A row's context menu (touch and hold on a phone; right click): a short list of commands under the row, or above it
 * near the bottom of the screen (HIG Context menus: "Aim for a small number of menu items"; HIG Menus: an ellipsis when
 * more input follows). Closes on a choice, a tap outside, Escape or scrolling; focus goes to its first item and back
 * where it was afterwards.
 */
function RowMenu({
  rect,
  label,
  items,
  onClose,
}: {
  rect: DOMRect;
  label: string;
  items: { label: string; onSelect: () => void }[];
  onClose: () => void;
}) {
  const menu = useRef<HTMLDivElement>(null);
  // The latest onClose, so the listeners below are set once while the menu is open.
  const close = useRef(onClose);
  useEffect(() => {
    close.current = onClose;
  });
  useEffect(() => {
    const onClose = () => close.current();
    const before = document.activeElement as HTMLElement | null;
    menu.current?.querySelector<HTMLElement>('[role="menuitem"]')?.focus({ preventScroll: true });
    const outside = (event: globalThis.PointerEvent) => {
      if (!menu.current?.contains(event.target as Node)) onClose();
    };
    const key = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    // The finger that opened it is still lifting: listen for taps outside from the next frame.
    const frame = window.requestAnimationFrame(() => document.addEventListener('pointerdown', outside));
    document.addEventListener('keydown', key);
    window.addEventListener('scroll', onClose, { passive: true });
    return () => {
      window.cancelAnimationFrame(frame);
      document.removeEventListener('pointerdown', outside);
      document.removeEventListener('keydown', key);
      window.removeEventListener('scroll', onClose);
      before?.focus?.({ preventScroll: true });
    };
  }, []);
  const below = rect.bottom + 8 + 56 * items.length < window.innerHeight - 96;
  return (
    <div
      ref={menu}
      className={cx('pw-popover pw-menu pw-row-menu', !below && 'is-above')}
      role="menu"
      aria-label={label}
      style={{
        position: 'fixed',
        left: rect.left + 8,
        ...(below ? { top: rect.bottom + 8 } : { top: 'auto', bottom: window.innerHeight - rect.top + 8 }),
      }}
    >
      {items.map((item) => (
        <button
          key={item.label}
          type="button"
          role="menuitem"
          className="pw-menu-item"
          onClick={() => {
            onClose();
            item.onSelect();
          }}
        >
          <span className="pw-menu-check" aria-hidden="true">
            <CopyIcon />
          </span>
          {item.label}
        </button>
      ))}
    </div>
  );
}
