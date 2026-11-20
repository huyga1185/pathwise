import { useState } from 'react';
import { Link, useParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { auditSentence } from '@/shared/audit';
import { messageFor } from '@/shared/errors';
import { formatDate, formatDayTime, formatRate } from '@/shared/format';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  ActivityIndicator,
  Alert,
  DataTable,
  ErrorBanner,
  MoreMenu,
  SelectField,
  Sheet,
  Stat,
  StatGroup,
  StatusPill,
  StoreIcon,
  TextField,
  Toast,
  ToastDock,
  type RowMenuItem,
} from '@/shared/ui';
import { BackLink } from '@/shared/ui/BackLink';
import { useAdminApi, useAdminAudit, useAdminStore, type AdminStoreDetail } from './adminApi';

const ZONE = 'Asia/Ho_Chi_Minh';
type Task = 'lock' | 'unlock' | 'transfer' | null;

/**
 * "Quản trị — chi tiết cửa hàng" (W11, T55–T58; redesigned in G7, OWNER_UI_REDESIGN §12): the standard back button,
 * the store's actions in a More menu on the title row (G7-Q3), numbers in groups, its members and latest log entries
 * in groups; locking and changing the owner in sheets, unlocking confirmed by an alert, the result in a toast.
 */
export function StoreDetailPage() {
  const id = Number(useParams().id);
  const store = useAdminStore(id);
  const t = vi.admin.store;
  if (store.isError) {
    const notFound = store.error instanceof ApiError && store.error.status === 404;
    return (
      <section className="pw-page-section">
        <BackLink to="/admin/stores" label={vi.admin.stores.title} />
        {notFound ? (
          <section className="pw-list-empty" aria-labelledby="store-missing-title">
            <span className="pw-list-empty-symbol" aria-hidden="true">
              <StoreIcon />
            </span>
            <h1 id="store-missing-title">{t.notFound}</h1>
          </section>
        ) : (
          <ErrorBanner title={messageFor(store.error)} />
        )}
      </section>
    );
  }
  if (!store.data) return <ActivityIndicator label={vi.pages.loading} className="is-page" />;
  return <StoreDetail store={store.data} />;
}

function StoreDetail({ store }: { store: AdminStoreDetail }) {
  const t = vi.admin.store;
  const wide = useMediaQuery(WIDE_QUERY);
  const adminApi = useAdminApi();
  const audit = useAdminAudit({ storeId: store.id, page: 0 }, 10);
  const [task, setTask] = useState<Task>(null);
  const [reason, setReason] = useState(store.lockReason ?? '');
  const candidates = store.members.filter((member) => member.status === 'ACTIVE' && member.role !== 'OWNER');
  const [newOwner, setNewOwner] = useState('');
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const [toast, setToast] = useState('');
  const locked = store.status === 'READ_ONLY';
  const owner = store.ownerEmail ?? t.noOwner;
  const preset = new URLSearchParams({ storeId: String(store.id), storeName: store.name }).toString();

  const open = (next: Task) => {
    setFailure(null);
    if (next === 'transfer') setNewOwner(String(candidates[0]?.id ?? ''));
    if (next === 'lock') setReason(store.lockReason ?? '');
    setTask(next);
  };
  const run = async (action: () => Promise<string>) => {
    setBusy(true);
    setFailure(null);
    try {
      const done = await action();
      setTask(null);
      setToast(done);
    } catch (error) {
      setFailure(error);
    } finally {
      setBusy(false);
    }
  };
  const reasonError = failure instanceof ApiError && failure.code === 'VALIDATION_ERROR' ? t.reasonRequired : null;

  // The store's actions (G7-Q3; HIG Toolbars "More menu"): the destructive one last, in red (HIG Menus).
  const actions: RowMenuItem[] = [
    { label: t.transfer, onSelect: () => open('transfer') },
    locked
      ? { label: t.unlock, onSelect: () => open('unlock') }
      : { label: t.lock, destructive: true, onSelect: () => open('lock') },
  ];

  return (
    <section className="pw-page-section" aria-labelledby="store-title">
      <BackLink to="/admin/stores" label={vi.admin.stores.title} />
      <div className="pw-admin-intro">
        <div className="pw-list-head pw-admin-title-head">
          <h1 id="store-title" className="pw-page-title">
            {store.name}
          </h1>
          <MoreMenu label={t.actions} items={actions} />
        </div>
        <div className="pw-admin-title-row">
          <StatusPill kind="store" status={store.status} />
          <p className="pw-muted pw-admin-meta">
            {t.meta(
              owner,
              formatDate(store.createdAt, ZONE),
              store.timezone,
              store.lastActivityAt ? formatDayTime(store.lastActivityAt, ZONE) : t.never,
            )}
          </p>
        </div>
      </div>
      {locked ? (
        <div className="pw-lock-banner" role="status">
          <strong>{t.lockedTitle}</strong>
          {store.lockReason ? <span>{t.reason(store.lockReason)}</span> : null}
          <span className="pw-muted">{t.lockedText(store.lockedAt ? formatDayTime(store.lockedAt, ZONE) : '')}</span>
        </div>
      ) : null}

      <StatGroup label={t.totals}>
        <Stat
          label={t.members}
          value={store.memberCounts.owners + store.memberCounts.dispatchers + store.memberCounts.drivers}
          sub={t.membersSub(store.memberCounts.owners, store.memberCounts.dispatchers, store.memberCounts.drivers)}
        />
        <Stat label={t.vehicles} value={store.vehicles} />
        <Stat label={t.plans} value={store.plans} sub={t.plansSub} />
      </StatGroup>
      <section className="pw-admin-block" aria-labelledby="last30">
        <h2 id="last30" className="pw-section-title">
          {t.last30}
        </h2>
        <StatGroup label={t.last30}>
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
        </StatGroup>
      </section>

      <section className="pw-admin-block" aria-labelledby="members-title">
        <div className="pw-admin-section-head">
          <h2 id="members-title" className="pw-section-title">
            {t.members}
          </h2>
          {/* The store as a preset filter there, its name to show (G7-Q2). */}
          <Link to={`/admin/users?${preset}`}>{t.usersAll}</Link>
        </div>
        {wide ? (
          <DataTable
            caption={t.members}
            className="pw-list-group"
            rows={store.members}
            columns={[
              {
                key: 'email',
                header: t.email,
                className: 'pw-admin-col-name',
                render: (row) => (
                  <span className="pw-cell-title" title={row.email}>
                    {row.email}
                  </span>
                ),
              },
              {
                key: 'role',
                header: t.role,
                className: 'pw-nowrap',
                render: (row) => vi.roles[row.role] ?? row.role,
              },
              { key: 'status', header: t.status, render: (row) => <StatusPill kind="member" status={row.status} /> },
              {
                key: 'joined',
                header: t.joined,
                numeric: true,
                className: 'pw-nowrap',
                render: (row) => formatDate(row.createdAt, ZONE),
              },
            ]}
          />
        ) : (
          // A phone: read-only rows, everything shown (no sheet to open, so nothing cut — HIG Typography).
          <ul className="pw-list-group pw-list-rows" aria-label={t.members}>
            {store.members.map((row) => (
              <li key={row.id} className="pw-list-row pw-admin-row">
                <span className="pw-list-row-main">
                  <span className="pw-admin-row-title">{row.email}</span>
                  <span className="pw-list-row-sub">
                    {t.memberLine(vi.roles[row.role] ?? row.role, formatDate(row.createdAt, ZONE))}
                  </span>
                </span>
                <StatusPill kind="member" status={row.status} />
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="pw-admin-block" aria-labelledby="audit-title">
        <div className="pw-admin-section-head">
          <h2 id="audit-title" className="pw-section-title">
            {t.audit}
          </h2>
          <Link to={`/admin/audit?${preset}`}>{t.auditAll}</Link>
        </div>
        {audit.isError ? <ErrorBanner title={messageFor(audit.error)} /> : null}
        {audit.data && audit.data.items.length === 0 ? <p className="pw-muted">{vi.audit.empty}</p> : null}
        {audit.data && audit.data.items.length > 0 ? (
          // The latest ten, whole sentences (not cut: no sheet here; the full log opens each, §8.10).
          <ol className="pw-list-group pw-list-rows" aria-label={t.audit}>
            {audit.data.items.map((event) => (
              <li key={event.id} className="pw-list-row pw-admin-row">
                <span className="pw-list-row-main">
                  <span className="pw-admin-row-title">{auditSentence(event)}</span>
                  <span className="pw-list-row-sub pw-tabular">
                    {[formatDayTime(event.occurredAt, ZONE), event.actorEmail].filter(Boolean).join(' · ')}
                  </span>
                </span>
              </li>
            ))}
          </ol>
        ) : null}
      </section>

      <Sheet
        open={task === 'lock'}
        title={t.lockSheet}
        doneLabel={t.lockConfirm}
        busy={busy}
        doneDisabled={reason.trim() === ''}
        dirty={reason.trim() !== (store.lockReason ?? '')}
        onClose={() => setTask(null)}
        onSubmit={(event) => {
          event.preventDefault();
          void run(async () => (await adminApi.lockStore(store.id, reason.trim()), t.locked));
        }}
      >
        <p className="pw-admin-sheet-name">{store.name}</p>
        <p>{t.lockText}</p>
        <TextField
          label={t.lockReason}
          multiline
          required
          maxLength={500}
          value={reason}
          hint={t.lockHint}
          error={reasonError ?? undefined}
          onChange={(event) => setReason(event.target.value)}
        />
        {failure && !reasonError ? <ErrorBanner title={messageFor(failure)} /> : null}
      </Sheet>
      <Sheet
        open={task === 'transfer'}
        title={t.transferSheet}
        doneLabel={t.transferConfirm}
        busy={busy}
        doneDisabled={candidates.length === 0}
        onClose={() => setTask(null)}
        onSubmit={(event) => {
          event.preventDefault();
          void run(async () => t.transferred((await adminApi.transferOwner(store.id, Number(newOwner))).ownerEmail));
        }}
      >
        <p className="pw-admin-sheet-name">{store.name}</p>
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
      </Sheet>
      <Alert
        open={task === 'unlock'}
        title={t.unlockTitle(store.name)}
        message={failure ? messageFor(failure) : t.unlockText}
        actionLabel={t.unlock}
        busy={busy}
        onCancel={() => setTask(null)}
        onAction={() => void run(async () => (await adminApi.unlockStore(store.id), t.unlocked))}
      />
      {toast ? (
        <ToastDock>
          <Toast tone="success" onClose={() => setToast('')}>
            {toast}
          </Toast>
        </ToastDock>
      ) : null}
    </section>
  );
}
