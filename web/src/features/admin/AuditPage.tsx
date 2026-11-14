import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { AUDIT_ACTION_LABELS, AUDIT_ACTIONS, auditSentence } from '@/shared/audit';
import { messageFor } from '@/shared/errors';
import { formatDayTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, DataTable, EmptyState, ErrorBanner, SelectField, TextField } from '@/shared/ui';
import { useAdminAudit, type AuditEvent, type AuditFilter } from './adminApi';
import { idOrUndefined } from './adminFormat';

const ZONE = 'Asia/Ho_Chi_Minh';

/** Prototype "Quản trị — nhật ký" (W11, T58): every event, newest first, filtered on the URL. Days in Vietnam. */
export function AuditPage() {
  const t = vi.audit;
  const [params, setParams] = useSearchParams();
  const filter: AuditFilter = {
    storeId: idOrUndefined(params.get('storeId')),
    actorId: idOrUndefined(params.get('actorId')),
    action: (params.get('action') as AuditFilter['action']) || undefined,
    from: params.get('from') ?? undefined,
    to: params.get('to') ?? undefined,
    page: Number(params.get('page') ?? 0),
  };
  const [storeText, setStoreText] = useState(params.get('storeId') ?? '');
  const [actorText, setActorText] = useState(params.get('actorId') ?? '');
  const audit = useAdminAudit(filter);
  const change = (
    next: Partial<Record<'storeId' | 'actorId' | 'action' | 'from' | 'to' | 'page', string | undefined>>,
  ) => {
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
    <section className="pw-page-section" aria-labelledby="audit-title">
      <div className="pw-admin-intro">
        <h1 id="audit-title" className="pw-page-title">
          {vi.admin.audit.title}
        </h1>
        <p className="pw-muted">{vi.admin.audit.lead}</p>
      </div>
      <form
        aria-label={vi.admin.audit.filterLabel}
        className="pw-admin-filters"
        onSubmit={(event) => {
          event.preventDefault();
          change({ storeId: idOrUndefined(storeText)?.toString(), actorId: idOrUndefined(actorText)?.toString() });
        }}
      >
        <TextField
          label={vi.admin.audit.storeId}
          type="number"
          min={1}
          value={storeText}
          onChange={(event) => setStoreText(event.target.value)}
          onBlur={() => change({ storeId: idOrUndefined(storeText)?.toString() })}
        />
        <TextField
          label={vi.admin.audit.actorId}
          type="number"
          min={1}
          value={actorText}
          onChange={(event) => setActorText(event.target.value)}
          onBlur={() => change({ actorId: idOrUndefined(actorText)?.toString() })}
        />
        <SelectField
          label={t.action}
          value={filter.action ?? ''}
          onChange={(event) => change({ action: event.target.value || undefined })}
          options={[
            { value: '', label: t.all },
            ...AUDIT_ACTIONS.map((action) => ({ value: action, label: AUDIT_ACTION_LABELS[action] ?? action })),
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
        <DataTable<AuditEvent>
          caption={vi.admin.audit.title}
          rows={page?.items ?? []}
          columns={[
            { key: 'at', header: t.at, render: (row) => formatDayTime(row.occurredAt, ZONE) },
            {
              key: 'actor',
              header: t.actor,
              render: (row) => (
                <div>
                  <div className="pw-admin-strong">{row.actorEmail ?? '—'}</div>
                  <div className="pw-admin-sub">{row.actorRole ? (vi.roles[row.actorRole] ?? row.actorRole) : ''}</div>
                </div>
              ),
            },
            { key: 'action', header: t.action, render: (row) => auditSentence(row) },
            { key: 'store', header: t.store, render: (row) => (row.storeId == null ? '—' : `#${row.storeId}`) },
            { key: 'ip', header: t.ip, render: (row) => row.ip ?? '—' },
          ]}
        />
      )}
      {page && page.items.length > 0 ? (
        <nav aria-label="Phân trang" className="pw-admin-pager">
          <span className="pw-muted">{t.page(page.page + 1, page.totalPages, page.totalElements)}</span>
          <div className="pw-admin-pager-buttons">
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
          </div>
        </nav>
      ) : null}
    </section>
  );
}
