import { Link, useNavigate, useSearchParams } from 'react-router';
import { useStoreQuery } from '@/data/storeQuery';
import { messageFor } from '@/shared/errors';
import { formatDateLong, formatDayTime, formatDistance, formatDuration } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import {
  Button,
  buttonClass,
  DataTable,
  ErrorBanner,
  SelectField,
  StatusPill,
  TextField,
  type DataTableColumn,
} from '@/shared/ui';
import { PAGE_SIZE, usePlans, type PlanStatus, type PlanSummary } from './planApi';
import './plans.css';

const STATUSES = new Set<string>(vi.plans.statusOptions.map((option) => option.value));
const DEVICE_TIME_ZONE = Intl.DateTimeFormat().resolvedOptions().timeZone;

/** "2026-12-01" → "01/12/2026". */
function shortDate(localDate: string): string {
  const [year, month, day] = localDate.split('-');
  return `${day}/${month}/${year}`;
}

/**
 * Prototype "Danh sách kế hoạch" (SCREENS §1.1): filters by date and status on the URL (`?date=&status=&page=`),
 * newest first, 20 a page, an empty state with "Tạo kế hoạch".
 */
export function PlanListPage() {
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const date = /^\d{4}-\d{2}-\d{2}$/.test(params.get('date') ?? '') ? (params.get('date') as string) : undefined;
  const rawStatus = params.get('status') ?? '';
  const status = STATUSES.has(rawStatus) ? (rawStatus as PlanStatus) : undefined;
  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0);
  const plans = usePlans({ date, status, page });
  const store = useStoreQuery();
  const timeZone = store.data?.timezone ?? DEVICE_TIME_ZONE;

  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)));
    if (!('page' in changes)) next.delete('page');
    setParams(next, { replace: true });
  };

  const columns: DataTableColumn<PlanSummary>[] = [
    {
      key: 'name',
      header: vi.plans.name,
      render: (row) => (
        <Link to={`/plans/${row.id}`} className="pw-plans-link" onClick={(event) => event.stopPropagation()}>
          {row.name}
        </Link>
      ),
    },
    {
      key: 'date',
      header: vi.plans.date,
      render: (row) => <span className="pw-tabular">{formatDateLong(row.planDate)}</span>,
    },
    { key: 'status', header: vi.plans.status, render: (row) => <StatusPill kind="plan" status={row.status} /> },
    { key: 'stops', header: vi.plans.stops, numeric: true, render: (row) => row.stopCount },
    { key: 'routes', header: vi.plans.routes, numeric: true, render: (row) => row.routeCount },
    {
      key: 'distance',
      header: vi.plans.distance,
      numeric: true,
      render: (row) => formatDistance(row.totalDistanceMeters),
    },
    {
      key: 'duration',
      header: vi.plans.duration,
      numeric: true,
      render: (row) => formatDuration(row.totalDurationSeconds),
    },
    {
      key: 'optimized',
      header: vi.plans.optimizedAt,
      numeric: true,
      render: (row) => (row.optimizedAt ? formatDayTime(row.optimizedAt, timeZone) : '—'),
    },
  ];

  const data = plans.data;
  const filtered = Boolean(date || status);
  const empty = data !== undefined && data.totalElements === 0;
  const from = data && data.items.length > 0 ? data.page * PAGE_SIZE + 1 : 0;
  const to = data ? data.page * PAGE_SIZE + data.items.length : 0;

  return (
    <section className="pw-page-section" aria-labelledby="plans-title">
      <div className="pw-plans-head">
        <h1 id="plans-title" className="pw-page-title">
          {vi.plans.title}
        </h1>
        {!empty ? (
          <Link className={buttonClass('primary')} to="/plans/new">
            {vi.plans.create}
          </Link>
        ) : null}
      </div>

      <form className="pw-plans-filter" aria-label={vi.plans.filterLabel} onSubmit={(event) => event.preventDefault()}>
        <TextField
          className="pw-plans-filter-date"
          label={vi.plans.date}
          type="date"
          value={date ?? ''}
          onChange={(event) => update({ date: event.target.value || undefined })}
        />
        <SelectField
          className="pw-plans-filter-status"
          label={vi.plans.status}
          placeholder={vi.plans.allStatuses}
          options={vi.plans.statusOptions}
          value={status ?? ''}
          onChange={(event) => update({ status: event.target.value || undefined })}
        />
        <Button variant="quiet" disabled={!filtered} onClick={() => setParams({}, { replace: true })}>
          {vi.plans.clearFilters}
        </Button>
      </form>

      {plans.isError ? <ErrorBanner title={messageFor(plans.error)} /> : null}

      {empty ? (
        <section className="pw-plans-empty" aria-labelledby="empty-title">
          <EmptyIllustration />
          <h2 id="empty-title">
            {date ? vi.plans.emptyTitleOn(shortDate(date)) : status ? vi.plans.emptyFiltered : vi.plans.emptyTitle}
          </h2>
          <p className="pw-muted">{vi.plans.emptyLead}</p>
          <Link className={buttonClass('primary')} to="/plans/new">
            {vi.plans.create}
          </Link>
        </section>
      ) : (
        <section className="pw-plans-list" aria-label={vi.plans.listLabel}>
          <DataTable
            caption={vi.plans.caption}
            columns={columns}
            rows={data?.items ?? []}
            onRowClick={(row) => navigate(`/plans/${row.id}`)}
            className="pw-plans-table"
            empty={vi.pages.loading}
          />
          {data && data.totalElements > 0 ? (
            <div className="pw-plans-pager">
              <span>{vi.plans.range(from, to, data.totalElements)}</span>
              <div className="pw-plans-pager-buttons">
                <Button variant="secondary" disabled={page === 0} onClick={() => update({ page: String(page - 1) })}>
                  {vi.plans.previous}
                </Button>
                <Button
                  variant="secondary"
                  disabled={page + 1 >= data.totalPages}
                  onClick={() => update({ page: String(page + 1) })}
                >
                  {vi.plans.next}
                </Button>
              </div>
            </div>
          ) : null}
        </section>
      )}
    </section>
  );
}

/** Prototype empty state: a small map with a dashed route and a pin. */
function EmptyIllustration() {
  return (
    <svg viewBox="0 0 240 150" width="240" height="150" aria-hidden="true" className="pw-plans-empty-art">
      <rect x="8" y="14" width="224" height="128" rx="8" className="pw-art-tint" />
      <path d="M8 62 H232 M8 104 H232 M70 14 V142 M150 14 V142" className="pw-art-street" strokeWidth="6" fill="none" />
      <path
        d="M40 120 C 70 120, 64 82, 100 80 S 150 52, 176 50"
        className="pw-art-route"
        strokeWidth="3"
        strokeDasharray="7 6"
        strokeLinecap="round"
        fill="none"
      />
      <circle cx="40" cy="120" r="8" className="pw-art-ink" />
      <circle cx="100" cy="80" r="7" className="pw-art-route-fill" />
      <path d="M178 58 s-14 -12.5 -14 -23 a14 14 0 0 1 28 0 c0 10.5 -14 23 -14 23z" className="pw-art-route-fill" />
      <circle cx="178" cy="35" r="5.5" className="pw-art-raised" />
    </svg>
  );
}
