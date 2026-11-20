import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { messageFor } from '@/shared/errors';
import { formatDate, formatDayTime } from '@/shared/format';
import { reducedMotion, useExit } from '@/shared/hooks/useExit';
import { useFitRows } from '@/shared/hooks/useFitRows';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  ChevronRightIcon,
  ClearFilters,
  DataTable,
  ErrorBanner,
  Pager,
  PlaceholderRows,
  PopUpFilter,
  SearchField,
  StatusPill,
  StoreIcon,
  type DataTableColumn,
} from '@/shared/ui';
import { cx } from '@/shared/ui/cx';
import { PAGE_SIZE, useAdminStores, type AdminStoreSummary, type StoreFilter } from './adminApi';

const ZONE = 'Asia/Ho_Chi_Minh';

/**
 * "Quản trị — cửa hàng" (W11, T55; redesigned in G7, OWNER_UI_REDESIGN §12) as the plan list (G2): a search field and
 * a status filter on one row, "Xoá bộ lọc" while filtering; on a wide window a table sorted by its "Cửa hàng"
 * heading, as many rows as fit with "‹ Trang x/y ›" under it; on a phone rows that open the store, 20 a page, the
 * pager above them. Filters, order and page live on the URL.
 */
export function StoresPage() {
  const t = vi.admin.stores;
  const navigate = useNavigate();
  const wide = useMediaQuery(WIDE_QUERY);
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const rawStatus = params.get('status');
  const status: StoreFilter['status'] = rawStatus === 'ACTIVE' || rawStatus === 'READ_ONLY' ? rawStatus : undefined;
  const sortParam: StoreFilter['sort'] = params.get('sort') === 'name,desc' ? 'name,desc' : 'name,asc';
  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0);

  const result = useRef<HTMLDivElement>(null);
  const probe = useRef<HTMLDivElement>(null);
  const filters = useRef<HTMLDivElement>(null);
  const fit = useFitRows(wide, result, probe, filters, PAGE_SIZE);
  const size = wide ? (fit?.rows ?? null) : PAGE_SIZE;
  const stores = useAdminStores({ q: q || undefined, status, page, sort: sortParam }, size ?? PAGE_SIZE, size !== null);

  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)));
    if (!('page' in changes)) next.delete('page'); // another search, filter or order starts at page 1 (G2.9)
    setParams(next, { replace: true });
  };
  const goToPage = (target: number) => {
    update({ page: target > 0 ? String(target) : undefined });
    // A phone: the next page starts at the top of the list, scrolled back only as far as needed (G2.9, §8.17).
    const top = result.current?.getBoundingClientRect().top ?? 0;
    const pinned = Number.parseFloat(getComputedStyle(document.documentElement).scrollPaddingTop) || 0;
    if (!wide && top < pinned) {
      result.current?.scrollIntoView({ block: 'start', behavior: reducedMotion() ? 'auto' : 'smooth' });
    }
  };
  // A wider or narrower window changes the rows a page holds: keep the first store shown on the page shown.
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

  const data = stores.data;
  const items = data?.items ?? [];
  const totalPages = data?.totalPages ?? 0;
  const filtered = Boolean(q || status);
  const loading = data === undefined && !stores.isError;
  const empty = data !== undefined && data.totalElements === 0;
  // A page past the end (an old link): the last page (G2.9).
  const pastEnd = data !== undefined && !stores.isPlaceholderData && totalPages > 0 && page >= totalPages;
  useEffect(() => {
    if (!pastEnd) return;
    const next = new URLSearchParams(params);
    if (totalPages > 1) next.set('page', String(totalPages - 1));
    else next.delete('page');
    setParams(next, { replace: true });
  }, [pastEnd, params, setParams, totalPages]);

  // The result's motion as the plan list's (G2.10): another search or filter settles in, another page slides in.
  const filterKey = [loading ? 'loading' : empty ? 'empty' : 'list', q, status, sortParam].join('|');
  const resultKey = `${filterKey}|${page}`;
  const [shown, setShown] = useState({ key: resultKey, filterKey, page, motion: '' });
  if (!stores.isPlaceholderData && resultKey !== shown.key) {
    const motion = filterKey !== shown.filterKey ? 'is-changed' : page > shown.page ? 'is-next' : 'is-prev';
    setShown({ key: resultKey, filterKey, page, motion });
  }
  const clearPresence = useExit(filtered, 140);

  const columns: DataTableColumn<AdminStoreSummary>[] = [
    {
      key: 'name',
      header: t.store,
      sortable: true,
      className: 'pw-admin-col-name',
      // Two lines at most, all of it on hover and in the store's page (G2.11; HIG Typography).
      render: (row) => (
        <>
          <Link
            to={`/admin/stores/${row.id}`}
            className="pw-cell-title pw-admin-link"
            title={row.name}
            onClick={(event) => event.stopPropagation()}
          >
            {row.name}
          </Link>
          <span className="pw-cell-sub" title={row.ownerEmail ?? undefined}>
            {row.ownerEmail ?? vi.admin.store.noOwner}
          </span>
          {/* A narrow table: members and dates fold under the name (G2.11). */}
          <span className="pw-cell-sub pw-tabular pw-admin-stores-fold">
            {t.foldLine(
              t.membersLine(row.memberCounts.owners, row.memberCounts.dispatchers, row.memberCounts.drivers),
              row.lastActivityAt ? formatDayTime(row.lastActivityAt, ZONE) : null,
              formatDate(row.createdAt, ZONE),
            )}
          </span>
        </>
      ),
    },
    { key: 'status', header: t.status, render: (row) => <StatusPill kind="store" status={row.status} /> },
    {
      key: 'members',
      header: t.members,
      numeric: true,
      className: 'pw-nowrap pw-admin-stores-col-fold',
      render: (row) => `${row.memberCounts.owners} / ${row.memberCounts.dispatchers} / ${row.memberCounts.drivers}`,
    },
    { key: 'vehicles', header: t.vehicles, numeric: true, render: (row) => row.vehicles },
    { key: 'plans', header: t.plans, numeric: true, render: (row) => row.plans },
    {
      key: 'activity',
      header: t.lastActivity,
      numeric: true,
      className: 'pw-nowrap pw-admin-stores-col-fold',
      render: (row) => (row.lastActivityAt ? formatDayTime(row.lastActivityAt, ZONE) : '—'),
    },
    {
      key: 'created',
      header: t.created,
      numeric: true,
      className: 'pw-nowrap pw-admin-stores-col-fold',
      render: (row) => formatDate(row.createdAt, ZONE),
    },
  ];

  return (
    <section className="pw-page-section" aria-labelledby="stores-title">
      <div className="pw-admin-intro">
        <h1 id="stores-title" className="pw-page-title">
          {t.title}
        </h1>
        <p className="pw-muted">{t.lead}</p>
      </div>

      <div ref={filters} className="pw-list-filters" role="group" aria-label={t.filterLabel}>
        <SearchField
          label={t.searchLabel}
          placeholder={t.search}
          value={q}
          clearText={t.clearSearch}
          onSearch={(value) => update({ q: value || undefined })}
        />
        <PopUpFilter
          label={t.status}
          value={status}
          emptyText={t.statusAll}
          options={[
            { value: 'ACTIVE', label: t.statusActive },
            { value: 'READ_ONLY', label: t.statusLocked },
          ]}
          onChange={(value) => update({ status: value })}
        />
        {clearPresence !== 'closed' ? (
          <ClearFilters
            text={vi.plans.clearFilters}
            closing={clearPresence === 'closing'}
            onClear={() => update({ q: undefined, status: undefined })}
          />
        ) : null}
      </div>

      {stores.isError ? <ErrorBanner title={messageFor(stores.error)} /> : null}

      {/* A row as tall as a real one can be (a two-line name, the email, the folded dates) measures a page (G2.9). */}
      <div ref={probe} className="pw-fit-probe" aria-hidden="true" inert>
        {wide ? (
          <>
            <div className="pw-list-group pw-table-wrap pw-admin-stores-table">
              <table className="pw-table">
                <thead>
                  <tr>
                    <th>{t.store}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="pw-admin-col-name">
                      <span className="pw-cell-title">
                        {t.store}
                        <br />
                        {t.store}
                      </span>
                      <span className="pw-cell-sub">{t.store}</span>
                      <span className="pw-cell-sub pw-admin-stores-fold">{t.store}</span>
                    </td>
                    <td>
                      <StatusPill kind="store" status="ACTIVE" />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <Pager label={t.pagerLabel} page={0} totalPages={2} onPage={() => {}} />
          </>
        ) : null}
      </div>
      {/* A phone: the pager above the list, pinned (§8.17); before it in reading and Tab order (WCAG 2.4.3). */}
      {!wide && data !== undefined && !empty ? (
        <Pager label={t.pagerLabel} page={page} totalPages={totalPages} onPage={goToPage} pinned />
      ) : null}
      <div
        ref={result}
        key={shown.key}
        className={cx('pw-list-result', shown.motion, stores.isPlaceholderData && 'is-stale')}
        style={wide && fit && !empty ? { minHeight: fit.height } : undefined}
        aria-busy={stores.isPlaceholderData || undefined}
      >
        {loading ? (
          <div className="pw-list-group">
            <PlaceholderRows rows={Math.min(size ?? 6, 12)} label={t.loading} />
          </div>
        ) : empty ? (
          <section className="pw-list-empty" aria-labelledby="stores-empty-title">
            <span className="pw-list-empty-symbol" aria-hidden="true">
              <StoreIcon />
            </span>
            <h2 id="stores-empty-title">{filtered ? t.empty : t.emptyNone}</h2>
            {filtered ? <p>{t.emptyHint}</p> : null}
          </section>
        ) : wide ? (
          <DataTable<AdminStoreSummary>
            caption={t.caption}
            columns={columns}
            rows={items}
            onRowClick={(row) => navigate(`/admin/stores/${row.id}`)}
            className="pw-list-group pw-admin-stores-table"
            sort={{ key: 'name', direction: sortParam === 'name,desc' ? 'desc' : 'asc' }}
            onSort={() => update({ sort: sortParam === 'name,desc' ? undefined : 'name,desc' })}
          />
        ) : (
          <ul className="pw-list-group pw-list-rows" aria-label={t.caption}>
            {items.map((row) => (
              <li key={row.id} className="pw-list-row">
                <Link to={`/admin/stores/${row.id}`} className="pw-list-row-main">
                  <span className="pw-list-row-title">{row.name}</span>
                  <span className="pw-list-row-sub">{row.ownerEmail ?? vi.admin.store.noOwner}</span>
                </Link>
                <StatusPill kind="store" status={row.status} />
                <span className="pw-list-row-chevron" aria-hidden="true">
                  <ChevronRightIcon />
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
      {/* Outside the result, drawn again on each page: the pager keeps its focus while pages turn. */}
      {wide && data !== undefined && !empty ? (
        <Pager label={t.pagerLabel} page={page} totalPages={totalPages} onPage={goToPage} />
      ) : null}
    </section>
  );
}
