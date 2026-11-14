import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { messageFor } from '@/shared/errors';
import { formatDate, formatDayTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, DataTable, EmptyState, ErrorBanner, SelectField, StatusPill, TextField } from '@/shared/ui';
import { useAdminStores, type AdminStoreSummary, type StoreFilter } from './adminApi';

const ZONE = 'Asia/Ho_Chi_Minh';

/** Prototype "Quản trị — cửa hàng" (W11, T55): search, filter, sort by name, pages. The filter lives on the URL. */
export function StoresPage() {
  const t = vi.admin.stores;
  const [params, setParams] = useSearchParams();
  const filter: StoreFilter = {
    q: params.get('q') ?? undefined,
    status: (params.get('status') as StoreFilter['status']) || undefined,
    page: Number(params.get('page') ?? 0),
    sort: params.get('sort') === 'name,desc' ? 'name,desc' : 'name,asc',
  };
  const [text, setText] = useState(filter.q ?? '');
  const stores = useAdminStores(filter);
  const change = (next: Partial<Record<'q' | 'status' | 'page' | 'sort', string | undefined>>) => {
    const merged = new URLSearchParams(params);
    for (const [key, value] of Object.entries(next)) {
      if (value) merged.set(key, value);
      else merged.delete(key);
    }
    if (!('page' in next)) merged.delete('page');
    setParams(merged, { replace: true });
  };
  const search = (event: FormEvent) => {
    event.preventDefault();
    change({ q: text.trim() || undefined });
  };
  const desc = filter.sort === 'name,desc';
  const page = stores.data;

  return (
    <section className="pw-page-section" aria-labelledby="stores-title">
      <div className="pw-admin-intro">
        <h1 id="stores-title" className="pw-page-title">
          {t.title}
        </h1>
        <p className="pw-muted">{t.lead}</p>
      </div>
      <form role="search" aria-label={t.searchLabel} className="pw-admin-filters" onSubmit={search}>
        <TextField label={t.search} type="search" value={text} onChange={(event) => setText(event.target.value)} />
        <SelectField
          label={t.status}
          value={filter.status ?? ''}
          onChange={(event) => change({ status: event.target.value || undefined })}
          options={[
            { value: '', label: t.statusAll },
            { value: 'ACTIVE', label: t.statusActive },
            { value: 'READ_ONLY', label: t.statusLocked },
          ]}
        />
      </form>
      {stores.isError ? <ErrorBanner title={messageFor(stores.error)} /> : null}
      {page && page.items.length === 0 ? (
        <EmptyState title={t.empty}>{t.emptyHint}</EmptyState>
      ) : (
        <DataTable<AdminStoreSummary>
          caption={t.caption}
          rows={page?.items ?? []}
          columns={[
            {
              key: 'store',
              header: (
                <button
                  type="button"
                  className="pw-admin-sort"
                  aria-label={t.sortBy(desc ? 'Z → A' : 'A → Z')}
                  onClick={() => change({ sort: desc ? 'name,asc' : 'name,desc' })}
                >
                  {t.store} {desc ? '↓' : '↑'}
                </button>
              ),
              render: (row) => (
                <div>
                  <Link to={`/admin/stores/${row.id}`} className="pw-admin-strong-link">
                    {row.name}
                  </Link>
                  <div className="pw-admin-sub">{row.ownerEmail ?? vi.admin.store.noOwner}</div>
                </div>
              ),
            },
            { key: 'status', header: t.status, render: (row) => <StatusPill kind="store" status={row.status} /> },
            {
              key: 'members',
              header: t.members,
              numeric: true,
              render: (row) =>
                `${row.memberCounts.owners} / ${row.memberCounts.dispatchers} / ${row.memberCounts.drivers}`,
            },
            { key: 'vehicles', header: t.vehicles, numeric: true, render: (row) => row.vehicles },
            { key: 'plans', header: t.plans, numeric: true, render: (row) => row.plans },
            {
              key: 'activity',
              header: t.lastActivity,
              render: (row) => (row.lastActivityAt ? formatDayTime(row.lastActivityAt, ZONE) : '—'),
            },
            { key: 'created', header: t.created, render: (row) => formatDate(row.createdAt, ZONE) },
          ]}
        />
      )}
      {page && page.items.length > 0 ? (
        <nav aria-label="Phân trang" className="pw-admin-pager">
          <span className="pw-muted">{t.count(page.totalElements, page.page + 1, page.totalPages)}</span>
          <div className="pw-admin-pager-buttons">
            <Button
              variant="secondary"
              disabled={page.page === 0}
              onClick={() => change({ page: String(page.page - 1) })}
            >
              {t.previous}
            </Button>
            <Button
              variant="secondary"
              disabled={page.page + 1 >= page.totalPages}
              onClick={() => change({ page: String(page.page + 1) })}
            >
              {t.next}
            </Button>
          </div>
        </nav>
      ) : null}
    </section>
  );
}
