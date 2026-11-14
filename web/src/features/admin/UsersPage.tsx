import { useState } from 'react';
import { useSearchParams } from 'react-router';
import { useSession } from '@/features/auth';
import { messageFor } from '@/shared/errors';
import { formatDate } from '@/shared/format';
import { vi } from '@/shared/i18n/vi';
import { Button, DataTable, Dialog, EmptyState, ErrorBanner, SelectField, StatusPill, TextField } from '@/shared/ui';
import { useAdminApi, useAdminUsers, type AdminUser, type UserFilter } from './adminApi';
import { idOrUndefined } from './adminFormat';

const ZONE = 'Asia/Ho_Chi_Minh';
type Action = 'verify' | 'reset' | 'revoke' | 'disable' | 'enable';

/**
 * Prototype "Quản trị — người dùng" (W11, T55–T57): filter by store, role, status (on the URL); disable, enable, log
 * out everywhere, resend the verification email, send a password reset link. Every action asks first; the ADMIN never
 * sees a code, a link or a password.
 */
export function UsersPage() {
  const t = vi.admin.users;
  const { session } = useSession();
  const meId = session.status === 'authenticated' ? session.user.id : null;
  const [params, setParams] = useSearchParams();
  const filter: UserFilter = {
    storeId: idOrUndefined(params.get('storeId')),
    role: (params.get('role') as UserFilter['role']) || undefined,
    status: (params.get('status') as UserFilter['status']) || undefined,
    page: Number(params.get('page') ?? 0),
  };
  const users = useAdminUsers(filter);
  const adminApi = useAdminApi();
  const [storeText, setStoreText] = useState(params.get('storeId') ?? '');
  const [pending, setPending] = useState<{ user: AdminUser; action: Action } | null>(null);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const [notice, setNotice] = useState('');

  const change = (next: Partial<Record<'storeId' | 'role' | 'status' | 'page', string | undefined>>) => {
    const merged = new URLSearchParams(params);
    for (const [key, value] of Object.entries(next)) {
      if (value) merged.set(key, value);
      else merged.delete(key);
    }
    if (!('page' in next)) merged.delete('page');
    setParams(merged, { replace: true });
  };
  const ask = (user: AdminUser, action: Action) => {
    setFailure(null);
    setNotice('');
    setPending({ user, action });
  };
  const confirm = async () => {
    if (!pending) return;
    const { user, action } = pending;
    setBusy(true);
    setFailure(null);
    try {
      const run = {
        verify: () => adminApi.sendVerificationEmail(user.id),
        reset: () => adminApi.sendPasswordResetLink(user.id),
        revoke: () => adminApi.revokeSessions(user.id),
        disable: () => adminApi.disableUser(user.id),
        enable: () => adminApi.enableUser(user.id),
      }[action];
      await run();
      setNotice(t.dialogs[action][3].replace('{e}', user.email));
      setPending(null);
    } catch (error) {
      setFailure(error);
    } finally {
      setBusy(false);
    }
  };
  const dialog = pending ? t.dialogs[pending.action] : null;
  const page = users.data;

  return (
    <section className="pw-page-section" aria-labelledby="users-title">
      <div className="pw-admin-intro">
        <h1 id="users-title" className="pw-page-title">
          {t.title}
        </h1>
        <p className="pw-muted">{t.lead}</p>
      </div>
      <form
        aria-label={t.filterLabel}
        className="pw-admin-filters"
        onSubmit={(event) => {
          event.preventDefault();
          change({ storeId: idOrUndefined(storeText)?.toString() });
        }}
      >
        <TextField
          label={t.storeId}
          type="number"
          min={1}
          value={storeText}
          hint={t.storeIdHint}
          onChange={(event) => setStoreText(event.target.value)}
          onBlur={() => change({ storeId: idOrUndefined(storeText)?.toString() })}
        />
        <SelectField
          label={t.role}
          value={filter.role ?? ''}
          onChange={(event) => change({ role: event.target.value || undefined })}
          options={[
            { value: '', label: t.all },
            ...(['OWNER', 'DISPATCHER', 'DRIVER', 'ADMIN'] as const).map((role) => ({
              value: role,
              label: vi.roles[role] ?? role,
            })),
          ]}
        />
        <SelectField
          label={t.status}
          value={filter.status ?? ''}
          onChange={(event) => change({ status: event.target.value || undefined })}
          options={[
            { value: '', label: t.all },
            { value: 'ACTIVE', label: t.statusActive },
            { value: 'PENDING_VERIFICATION', label: t.statusPending },
            { value: 'DISABLED', label: t.statusDisabled },
          ]}
        />
      </form>
      {notice ? (
        <p role="status" className="pw-admin-notice">
          {notice}
        </p>
      ) : null}
      {users.isError ? <ErrorBanner title={messageFor(users.error)} /> : null}
      {page && page.items.length === 0 ? (
        <EmptyState title={t.empty} />
      ) : (
        <DataTable<AdminUser>
          caption={t.caption}
          rows={page?.items ?? []}
          columns={[
            { key: 'email', header: t.email, render: (row) => <span className="pw-admin-strong">{row.email}</span> },
            { key: 'role', header: t.role, render: (row) => vi.roles[row.role] ?? row.role },
            { key: 'store', header: t.store, render: (row) => (row.storeId == null ? '—' : `#${row.storeId}`) },
            { key: 'status', header: t.status, render: (row) => <StatusPill kind="member" status={row.status} /> },
            { key: 'created', header: t.created, render: (row) => formatDate(row.createdAt, ZONE) },
            {
              key: 'actions',
              header: <span className="pw-visually-hidden">{t.actions}</span>,
              align: 'end',
              render: (row) => (
                <div className="pw-admin-row-actions">
                  {row.status === 'PENDING_VERIFICATION' ? (
                    <Button variant="quiet" onClick={() => ask(row, 'verify')}>
                      {t.verify}
                    </Button>
                  ) : null}
                  {row.status === 'ACTIVE' ? (
                    <Button variant="quiet" onClick={() => ask(row, 'reset')}>
                      {t.reset}
                    </Button>
                  ) : null}
                  {row.status !== 'DISABLED' ? (
                    <Button variant="quiet" onClick={() => ask(row, 'revoke')}>
                      {t.revoke}
                    </Button>
                  ) : null}
                  {row.status !== 'DISABLED' && row.id !== meId ? (
                    <Button
                      variant="quiet"
                      aria-label={`${t.disable} ${row.email}`}
                      onClick={() => ask(row, 'disable')}
                    >
                      {t.disable}
                    </Button>
                  ) : null}
                  {row.status === 'DISABLED' ? (
                    <Button variant="quiet" aria-label={`${t.enable} ${row.email}`} onClick={() => ask(row, 'enable')}>
                      {t.enable}
                    </Button>
                  ) : null}
                </div>
              ),
            },
          ]}
        />
      )}
      {page && page.items.length > 0 ? (
        <nav aria-label="Phân trang" className="pw-admin-pager">
          <span className="pw-muted">{vi.admin.stores.count(page.totalElements, page.page + 1, page.totalPages)}</span>
          <div className="pw-admin-pager-buttons">
            <Button
              variant="secondary"
              disabled={page.page === 0}
              onClick={() => change({ page: String(page.page - 1) })}
            >
              {vi.admin.stores.previous}
            </Button>
            <Button
              variant="secondary"
              disabled={page.page + 1 >= page.totalPages}
              onClick={() => change({ page: String(page.page + 1) })}
            >
              {vi.admin.stores.next}
            </Button>
          </div>
        </nav>
      ) : null}
      <Dialog
        open={pending !== null}
        tone={pending && (pending.action === 'disable' || pending.action === 'revoke') ? 'danger' : 'primary'}
        title={dialog?.[0] ?? ''}
        confirmLabel={dialog?.[1] ?? ''}
        cancelLabel={t.back}
        busy={busy}
        onClose={() => setPending(null)}
        onConfirm={() => void confirm()}
      >
        <p>{dialog && pending ? dialog[2].replace('{e}', pending.user.email) : ''}</p>
        {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      </Dialog>
    </section>
  );
}
