import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { auditSentence } from '@/shared/audit';
import { messageFor } from '@/shared/errors';
import { formatDate, formatDayTime } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, DataTable, Dialog, EmptyState, ErrorBanner, SelectField, StatusPill } from '@/shared/ui';
import { useAdminApi, useAdminAudit, useAdminStore, type AdminStoreDetail } from './adminApi';
import { formatRate } from './adminFormat';
import { Stat } from './Stat';

const ZONE = 'Asia/Ho_Chi_Minh';
type DialogKind = 'lock' | 'unlock' | 'transfer' | null;

/** Prototype "Quản trị — chi tiết cửa hàng" (W11, T55–T58): totals, members, lock, change of owner, its log. */
export function StoreDetailPage() {
  const id = Number(useParams().id);
  const store = useAdminStore(id);
  const t = vi.admin.store;
  if (store.isError) {
    const notFound = store.error instanceof ApiError && store.error.status === 404;
    return (
      <section className="pw-page-section">
        <Link to="/admin/stores">{t.back}</Link>
        {notFound ? <EmptyState title={t.notFound} /> : <ErrorBanner title={messageFor(store.error)} />}
      </section>
    );
  }
  if (!store.data) return <p role="status">{vi.pages.loading}</p>;
  return <StoreDetail store={store.data} />;
}

function StoreDetail({ store }: { store: AdminStoreDetail }) {
  const t = vi.admin.store;
  const adminApi = useAdminApi();
  const audit = useAdminAudit({ storeId: store.id, page: 0 }, 10);
  const [dialog, setDialog] = useState<DialogKind>(null);
  const [reason, setReason] = useState(store.lockReason ?? '');
  const candidates = store.members.filter((member) => member.status === 'ACTIVE' && member.role !== 'OWNER');
  const [newOwner, setNewOwner] = useState('');
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const [notice, setNotice] = useState('');
  const locked = store.status === 'READ_ONLY';
  const owner = store.ownerEmail ?? t.noOwner;

  const open = (kind: DialogKind) => {
    setFailure(null);
    setNotice('');
    if (kind === 'transfer') setNewOwner(String(candidates[0]?.id ?? ''));
    setDialog(kind);
  };
  const run = async (action: () => Promise<string>) => {
    setBusy(true);
    setFailure(null);
    try {
      setNotice(await action());
      setDialog(null);
    } catch (error) {
      setFailure(error);
    } finally {
      setBusy(false);
    }
  };
  const reasonError = failure instanceof ApiError && failure.code === 'VALIDATION_ERROR' ? t.reasonRequired : null;

  return (
    <section className="pw-page-section" aria-labelledby="store-title">
      <Link to="/admin/stores" className="pw-admin-strong-link">
        {t.back}
      </Link>
      <div className="pw-admin-head">
        <div className="pw-admin-intro">
          <div className="pw-admin-title-row">
            <h1 id="store-title" className="pw-page-title">
              {store.name}
            </h1>
            <StatusPill kind="store" status={store.status} />
          </div>
          <p className="pw-muted">
            {t.meta(
              owner,
              formatDate(store.createdAt, ZONE),
              store.timezone,
              store.lastActivityAt ? formatDayTime(store.lastActivityAt, ZONE) : t.never,
            )}
          </p>
        </div>
        <div className="pw-admin-actions">
          {locked ? (
            <Button variant="secondary" onClick={() => open('unlock')}>
              {t.unlock}
            </Button>
          ) : (
            <Button variant="secondary" onClick={() => open('lock')}>
              {t.lock}
            </Button>
          )}
          <Button variant="secondary" onClick={() => open('transfer')}>
            {t.transfer}
          </Button>
        </div>
      </div>
      {locked ? (
        <div className="pw-lock-banner" role="status">
          <strong>{t.lockedTitle}</strong>
          {store.lockReason ? <span>{t.reason(store.lockReason)}</span> : null}
          <span className="pw-muted">{t.lockedText(store.lockedAt ? formatDayTime(store.lockedAt, ZONE) : '')}</span>
        </div>
      ) : null}
      {notice ? (
        <p role="status" className="pw-admin-notice">
          {notice}
        </p>
      ) : null}

      <div className="pw-admin-grid">
        <Stat
          label={t.members}
          value={store.memberCounts.owners + store.memberCounts.dispatchers + store.memberCounts.drivers}
          sub={t.membersSub(store.memberCounts.owners, store.memberCounts.dispatchers, store.memberCounts.drivers)}
        />
        <Stat label={t.vehicles} value={store.vehicles} />
        <Stat label={t.plans} value={store.plans} sub={t.plansSub} />
      </div>
      <section className="pw-admin-block" aria-labelledby="last30">
        <h2 id="last30" className="pw-admin-h2">
          {t.last30}
        </h2>
        <div className="pw-admin-grid">
          <Stat
            label={t.plans}
            value={store.last30Days.plansPublished}
            sub={t.plans30Sub(store.last30Days.plansCreated, store.last30Days.plansCompleted)}
          />
          <Stat
            label={vi.admin.overview.delivered}
            value={formatRate(store.last30Days.deliveredRate)}
            sub={vi.admin.overview.stopsSub(
              store.last30Days.stopsDelivered,
              store.last30Days.stopsFailed,
              store.last30Days.stopsSkipped,
            )}
          />
        </div>
      </section>
      <section className="pw-admin-block" aria-labelledby="members-title">
        <h2 id="members-title" className="pw-admin-h2">
          {t.members}
        </h2>
        <DataTable
          caption={t.members}
          rows={store.members}
          columns={[
            { key: 'email', header: t.email, render: (row) => row.email },
            { key: 'role', header: t.role, render: (row) => vi.roles[row.role] ?? row.role },
            { key: 'status', header: t.status, render: (row) => <StatusPill kind="member" status={row.status} /> },
            { key: 'joined', header: t.joined, render: (row) => formatDate(row.createdAt, ZONE) },
          ]}
        />
      </section>
      <section className="pw-admin-block" aria-labelledby="audit-title">
        <div className="pw-admin-head">
          <h2 id="audit-title" className="pw-admin-h2">
            {t.audit}
          </h2>
          <Link to={`/admin/audit?storeId=${store.id}`}>{t.auditAll}</Link>
        </div>
        {audit.isError ? <ErrorBanner title={messageFor(audit.error)} /> : null}
        <ol className="pw-admin-log">
          {(audit.data?.items ?? []).map((event) => (
            <li key={event.id}>
              <span className="pw-muted">{formatDayTime(event.occurredAt, ZONE)}</span>
              <span>
                {event.actorEmail ? `${event.actorEmail}: ` : ''}
                {auditSentence(event)}
              </span>
            </li>
          ))}
        </ol>
        {audit.data && audit.data.items.length === 0 ? <p className="pw-muted">{vi.audit.empty}</p> : null}
      </section>

      <Dialog
        open={dialog === 'lock'}
        tone="danger"
        title={t.lockTitle(store.name)}
        confirmLabel={t.lockConfirm}
        cancelLabel={t.back2}
        busy={busy}
        confirmDisabled={reason.trim() === ''}
        onClose={() => setDialog(null)}
        onConfirm={() => void run(async () => (await adminApi.lockStore(store.id, reason.trim()), t.locked))}
      >
        <p>{t.lockText}</p>
        <div className="pw-field">
          <label className="pw-field-label" htmlFor="lock-reason">
            {t.lockReason}{' '}
            <span className="pw-field-req" aria-hidden="true">
              *
            </span>
          </label>
          <textarea
            id="lock-reason"
            className="pw-field-input"
            rows={3}
            maxLength={500}
            required
            value={reason}
            aria-describedby="lock-hint"
            aria-invalid={reasonError ? true : undefined}
            onChange={(event) => setReason(event.target.value)}
          />
          <p id="lock-hint" className="pw-field-msg">
            {t.lockHint}
          </p>
        </div>
        {failure ? <ErrorBanner title={reasonError ?? messageFor(failure)} /> : null}
      </Dialog>
      <Dialog
        open={dialog === 'unlock'}
        title={t.unlockTitle(store.name)}
        confirmLabel={t.unlock}
        cancelLabel={t.back2}
        busy={busy}
        onClose={() => setDialog(null)}
        onConfirm={() => void run(async () => (await adminApi.unlockStore(store.id), t.unlocked))}
      >
        <p>{t.unlockText}</p>
        {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      </Dialog>
      <Dialog
        open={dialog === 'transfer'}
        tone="danger"
        title={t.transferTitle(store.name)}
        confirmLabel={t.transferConfirm}
        cancelLabel={t.back2}
        busy={busy}
        confirmDisabled={candidates.length === 0}
        onClose={() => setDialog(null)}
        onConfirm={() =>
          void run(async () => {
            const result = await adminApi.transferOwner(store.id, Number(newOwner));
            return t.transferred(result.ownerEmail);
          })
        }
      >
        {candidates.length === 0 ? (
          <p>{t.noCandidate}</p>
        ) : (
          <SelectField
            label={t.newOwner}
            required
            hint={t.newOwnerHint}
            value={newOwner}
            onChange={(event) => setNewOwner(event.target.value)}
            options={candidates.map((member) => ({
              value: String(member.id),
              label: `${member.email} — ${vi.roles[member.role] ?? member.role}`,
            }))}
          />
        )}
        <p>{t.transferText(owner)}</p>
        {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      </Dialog>
    </section>
  );
}
