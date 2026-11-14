import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useSearchParams } from 'react-router';
import { useApi } from '@/data/ApiContext';
import { unwrap } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import { useStoreQuery } from '@/data/storeQuery';
import type { Schemas } from '@/data/types';
import { AUDIT_ACTION_LABELS, AUDIT_ACTIONS, auditSentence } from '@/shared/audit';
import { messageFor } from '@/shared/errors';
import { formatDayTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, DataTable, EmptyState, ErrorBanner, SelectField, TextField } from '@/shared/ui';
import { StoreTabs } from './StoreTabs';

type StoreAuditEvent = Schemas['StoreAuditEventResponse'];
const DEVICE_TIME_ZONE = Intl.DateTimeFormat().resolvedOptions().timeZone;

/**
 * Prototype "Nhật ký cửa hàng" (W11, T58): the OWNER's own store log, newest first, days in the store's time zone. An
 * ADMIN's IP is never there (the server leaves it out).
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

  return (
    <section className="pw-page-section" aria-labelledby="store-audit-title">
      <h1 id="store-audit-title" className="pw-page-title">
        {vi.store.title}
      </h1>
      <StoreTabs />
      <p className="pw-muted">{t.storeLead}</p>
      <form className="pw-store-audit-filters" aria-label={t.title} onSubmit={(event) => event.preventDefault()}>
        <SelectField
          label={t.action}
          value={filter.action ?? ''}
          onChange={(event) => change({ action: event.target.value || undefined })}
          options={[
            { value: '', label: t.all },
            ...AUDIT_ACTIONS.map((a) => ({ value: a, label: AUDIT_ACTION_LABELS[a] ?? a })),
          ]}
        />
        <TextField
          label={t.from}
          type="date"
          value={filter.from ?? ''}
          onChange={(event) => change({ from: event.target.value })}
        />
        <TextField
          label={t.to}
          type="date"
          value={filter.to ?? ''}
          onChange={(event) => change({ to: event.target.value })}
        />
      </form>
      {audit.isError ? <ErrorBanner title={messageFor(audit.error)} /> : null}
      {page && page.items.length === 0 ? (
        <EmptyState title={t.empty} />
      ) : (
        <DataTable<StoreAuditEvent>
          caption={t.title}
          rows={page?.items ?? []}
          columns={[
            { key: 'at', header: t.at, render: (row) => formatDayTime(row.occurredAt, timeZone) },
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
          ]}
        />
      )}
      {page && page.items.length > 0 ? (
        <nav aria-label="Phân trang" className="pw-store-audit-pager">
          <span className="pw-muted">{t.page(page.page + 1, page.totalPages, page.totalElements)}</span>
          <span className="pw-store-audit-pager-buttons">
            <Button
              variant="secondary"
              disabled={page.page === 0}
              onClick={() => change({ page: String(page.page - 1) })}
            >
              {t.newer}
            </Button>
            <Button
              variant="secondary"
              disabled={page.page + 1 >= page.totalPages}
              onClick={() => change({ page: String(page.page + 1) })}
            >
              {t.older}
            </Button>
          </span>
        </nav>
      ) : null}
    </section>
  );
}
