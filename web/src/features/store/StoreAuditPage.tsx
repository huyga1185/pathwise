import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router';
import { useApi } from '@/data/ApiContext';
import { unwrap } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import { useStoreQuery } from '@/data/storeQuery';
import type { Schemas } from '@/data/types';
import { AUDIT_ACTION_LABELS, AUDIT_ACTIONS, auditSentence } from '@/shared/audit';
import { messageFor } from '@/shared/errors';
import { formatDayTime, todayIn } from '@/shared/format';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  DataTable,
  DatePickerFilter,
  EmptyState,
  ErrorBanner,
  Pager,
  PlaceholderRows,
  PopUpFilter,
  type DataTableColumn,
} from '@/shared/ui';
import { StoreTabs } from './StoreTabs';

type StoreAuditEvent = Schemas['StoreAuditEventResponse'];
const DEVICE_TIME_ZONE = Intl.DateTimeFormat().resolvedOptions().timeZone;

/**
 * "Nhật ký cửa hàng" (W11, T58; OWNER_UI_REDESIGN §8.3): the OWNER's own store log, newest first, days in the store's
 * time zone; 20 a page with the shared pager (`?page=`). Filters as on the plan list; a table on a wide window, rows
 * (what happened, then when · who) on a phone. An ADMIN's IP is never there (the server leaves it out).
 */
export function StoreAuditPage() {
  const api = useApi();
  const t = vi.audit;
  const [params, setParams] = useSearchParams();
  const filter = {
    action: (params.get('action') as StoreAuditEvent['action']) || undefined,
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
    page: Number(params.get('page') ?? 0),
  };
  const store = useStoreQuery();
  const timeZone = store.data?.timezone ?? DEVICE_TIME_ZONE;
  const audit = useQuery({
    queryKey: queryKeys.storeAudit(filter),
    queryFn: () =>
      unwrap(
        api.GET('/api/v1/store/audit', {
          params: {
            query: {
              ...filter,
              from: filter.from || undefined,
              to: filter.to || undefined,
              size: 20,
              sort: ['occurredAt,desc'],
            },
          },
        }),
      ),
    placeholderData: keepPreviousData,
  });
  const change = (next: Partial<Record<'action' | 'from' | 'to' | 'page', string | undefined>>) => {
    const merged = new URLSearchParams(params);
    for (const [key, value] of Object.entries(next)) {
      if (value) merged.set(key, value);
      else merged.delete(key);
    }
    if (!('page' in next)) merged.delete('page');
    setParams(merged, { replace: true });
  };
  const page = audit.data;
  const wide = useMediaQuery(WIDE_QUERY);
  const actor = (row: StoreAuditEvent) => (row.actorRole === 'ADMIN' ? t.admin : (row.actorEmail ?? '—'));
  const columns: DataTableColumn<StoreAuditEvent>[] = [
    {
      key: 'at',
      header: t.at,
      render: (row) => <span className="pw-nowrap pw-tabular">{formatDayTime(row.occurredAt, timeZone)}</span>,
    },
    {
      key: 'actor',
      header: t.actor,
      render: (row) =>
        row.actorRole === 'ADMIN' ? (
          t.admin
        ) : (
          <div>
            <div className="pw-store-audit-actor">{row.actorEmail ?? '—'}</div>
            <div className="pw-muted">{row.actorRole ? (vi.roles[row.actorRole] ?? row.actorRole) : ''}</div>
          </div>
        ),
    },
    { key: 'action', header: t.action, render: (row) => auditSentence(row) },
    { key: 'ip', header: t.ip, render: (row) => row.ip ?? '—' },
  ];

  return (
    <section className="pw-page-section" aria-labelledby="store-audit-title">
      <h1 id="store-audit-title" className="pw-page-title">
        {vi.store.title}
      </h1>
      <StoreTabs />
      <p className="pw-muted pw-list-lead-tight">{t.storeLead}</p>
      {/* Filters like the plan list (G2): pop-up button and compact date pickers, no card. */}
      <div className="pw-list-filters" role="group" aria-label={t.filterLabel}>
        <PopUpFilter
          label={t.action}
          value={filter.action}
          emptyText={t.all}
          options={AUDIT_ACTIONS.map((a) => ({ value: a, label: AUDIT_ACTION_LABELS[a] ?? a }))}
          onChange={(value) => change({ action: value })}
        />
        <DatePickerFilter
          label={t.from}
          value={filter.from}
          emptyText={t.any}
          today={todayIn(timeZone)}
          clearText={t.anyDate}
          previousMonthText={vi.plans.previousMonth}
          nextMonthText={vi.plans.nextMonth}
          onChange={(value) => change({ from: value })}
        />
        <DatePickerFilter
          label={t.to}
          value={filter.to}
          emptyText={t.any}
          today={todayIn(timeZone)}
          clearText={t.anyDate}
          previousMonthText={vi.plans.previousMonth}
          nextMonthText={vi.plans.nextMonth}
          onChange={(value) => change({ to: value })}
        />
      </div>
      {audit.isError ? <ErrorBanner title={messageFor(audit.error)} /> : null}
      {!page && !audit.isError ? (
        <div className="pw-list-group">
          <PlaceholderRows rows={6} label={t.loading} />
        </div>
      ) : page && page.items.length === 0 ? (
        <EmptyState title={t.empty} />
      ) : wide ? (
        <DataTable<StoreAuditEvent>
          caption={t.title}
          rows={page?.items ?? []}
          columns={columns}
          resizable
          className="pw-list-group"
        />
      ) : (
        <ul className="pw-list-group pw-list-rows" aria-label={t.title}>
          {(page?.items ?? []).map((row) => (
            <li key={row.id} className="pw-list-row">
              <span className="pw-list-row-main">
                <span className="pw-list-row-title pw-store-audit-sentence">{auditSentence(row)}</span>
                <span className="pw-list-row-sub">
                  {formatDayTime(row.occurredAt, timeZone)} · {actor(row)}
                </span>
              </span>
            </li>
          ))}
        </ul>
      )}
      {page ? (
        <Pager
          label={t.pagerLabel}
          page={page.page}
          totalPages={page.totalPages}
          onPage={(next) => change({ page: next > 0 ? String(next) : undefined })}
        />
      ) : null}
    </section>
  );
}
