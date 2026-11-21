import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router';
import { useSession } from '@/features/auth';
import { messageFor } from '@/shared/errors';
import { formatDate } from '@/shared/format';
import { reducedMotion, useExit } from '@/shared/hooks/useExit';
import { useFitRows } from '@/shared/hooks/useFitRows';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  Alert,
  ClearFilters,
  DataTable,
  EllipsisIcon,
  ErrorBanner,
  Pager,
  PlaceholderRows,
  PopUpFilter,
  PresetFilter,
  RowMenu,
  StatusPill,
  Toast,
  ToastDock,
  UsersIcon,
  useRowMenu,
  type DataTableColumn,
  type RowMenuItem,
} from '@/shared/ui';
import { cx } from '@/shared/ui/cx';
import { PAGE_SIZE, useAdminApi, useAdminUsers, type AdminUser, type UserFilter } from './adminApi';
import { idOrUndefined } from './adminFormat';

const ZONE = 'Asia/Ho_Chi_Minh';
type Action = 'verify' | 'reset' | 'revoke' | 'disable' | 'enable';
const ROLES = ['OWNER', 'DISPATCHER', 'DRIVER', 'ADMIN'] as const;
const STATUSES = ['ACTIVE', 'PENDING_VERIFICATION', 'DISABLED'] as const;

/**
 * "Quản trị — người dùng" (W11, T55–T57; redesigned in G7, OWNER_UI_REDESIGN §12) as the plan list: role and status
 * filters on one row, the store only as a preset from its page (G7-Q2); a table that fits the window, rows on a
 * phone; each row's actions in a "…" menu at its end (G7-Q1), also on a right click or a touch and hold (G2.13).
 * Every action asks first in an alert; the result shows in a toast. The ADMIN never sees a code, a link or a password.
 */
export function UsersPage() {
  const t = vi.admin.users;
  const { session } = useSession();
  const meId = session.status === 'authenticated' ? session.user.id : null;
  const navigate = useNavigate();
  const wide = useMediaQuery(WIDE_QUERY);
  const [params, setParams] = useSearchParams();
  const storeId = idOrUndefined(params.get('storeId'));
  const storeName = params.get('storeName');
  const rawRole = params.get('role') ?? '';
  const role = (ROLES as readonly string[]).includes(rawRole) ? (rawRole as UserFilter['role']) : undefined;
  const rawStatus = params.get('status') ?? '';
  const status = (STATUSES as readonly string[]).includes(rawStatus) ? (rawStatus as UserFilter['status']) : undefined;
  const page = Math.max(0, Number.parseInt(params.get('page') ?? '0', 10) || 0);

  const result = useRef<HTMLDivElement>(null);
  const probe = useRef<HTMLDivElement>(null);
  const filters = useRef<HTMLDivElement>(null);
  const fit = useFitRows(wide, result, probe, filters, PAGE_SIZE);
  const size = wide ? (fit?.rows ?? null) : PAGE_SIZE;
  const users = useAdminUsers({ storeId, role, status, page }, size ?? PAGE_SIZE, size !== null);
  const adminApi = useAdminApi();
  const rowMenu = useRowMenu<AdminUser>();
  const [pending, setPending] = useState<{ user: AdminUser; action: Action } | null>(null);
  const [busy, setBusy] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const [toast, setToast] = useState('');

  const update = (changes: Record<string, string | undefined>) => {
    const next = new URLSearchParams(params);
    Object.entries(changes).forEach(([key, value]) => (value ? next.set(key, value) : next.delete(key)));
    if (!('page' in changes)) next.delete('page'); // another filter starts at page 1 (G2.9)
    setParams(next, { replace: true });
  };
  const goToPage = (target: number) => {
    update({ page: target > 0 ? String(target) : undefined });
    const top = result.current?.getBoundingClientRect().top ?? 0;
    const pinned = Number.parseFloat(getComputedStyle(document.documentElement).scrollPaddingTop) || 0;
    if (!wide && top < pinned) {
      result.current?.scrollIntoView({ block: 'start', behavior: reducedMotion() ? 'auto' : 'smooth' });
    }
  };
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

  const ask = (user: AdminUser, action: Action) => {
    setFailure(null);
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
      setToast(t.dialogs[action][3].replace('{e}', user.email));
      setPending(null);
    } catch (error) {
      setFailure(error);
    } finally {
      setBusy(false);
    }
  };
  // What a user's menu offers by their state (G7-Q1): the destructive "Khoá" last, in red (HIG Menus); never oneself.
  const menuItems = (row: AdminUser): RowMenuItem[] => [
    ...(row.status === 'PENDING_VERIFICATION' ? [{ label: t.verify, onSelect: () => ask(row, 'verify') }] : []),
    ...(row.status === 'ACTIVE' ? [{ label: t.reset, onSelect: () => ask(row, 'reset') }] : []),
    { label: t.viewLog, onSelect: () => navigate(auditLink(row)) },
    ...(row.status !== 'DISABLED' ? [{ label: t.revoke, onSelect: () => ask(row, 'revoke') }] : []),
    ...(row.status === 'DISABLED' ? [{ label: t.enable, onSelect: () => ask(row, 'enable') }] : []),
    ...(row.status !== 'DISABLED' && row.id !== meId
      ? [{ label: t.disable, destructive: true, onSelect: () => ask(row, 'disable') }]
      : []),
  ];
  const menuButton = (row: AdminUser) => (
    <button
      type="button"
      className="pw-icon-button pw-more-button"
      aria-label={t.actionsOf(row.email)}
      title={t.actions}
      aria-haspopup="menu"
      aria-expanded={rowMenu.menu?.row.id === row.id}
      onClick={(event) => {
        event.stopPropagation();
        rowMenu.open(row, event.currentTarget.getBoundingClientRect());
      }}
    >
      <EllipsisIcon />
    </button>
  );

  const data = users.data;
  const items = data?.items ?? [];
  const totalPages = data?.totalPages ?? 0;
  const filtered = Boolean(storeId || role || status);
  const loading = data === undefined && !users.isError;
  const empty = data !== undefined && data.totalElements === 0;
  const pastEnd = data !== undefined && !users.isPlaceholderData && totalPages > 0 && page >= totalPages;
  useEffect(() => {
    if (!pastEnd) return;
    const next = new URLSearchParams(params);
    if (totalPages > 1) next.set('page', String(totalPages - 1));
    else next.delete('page');
    setParams(next, { replace: true });
  }, [pastEnd, params, setParams, totalPages]);
  const filterKey = [loading ? 'loading' : empty ? 'empty' : 'list', storeId, role, status].join('|');
  const resultKey = `${filterKey}|${page}`;
  const [shown, setShown] = useState({ key: resultKey, filterKey, page, motion: '' });
  if (!users.isPlaceholderData && resultKey !== shown.key) {
    const motion = filterKey !== shown.filterKey ? 'is-changed' : page > shown.page ? 'is-next' : 'is-prev';
    setShown({ key: resultKey, filterKey, page, motion });
  }
  const clearPresence = useExit(filtered, 140);
  const dialog = pending ? t.dialogs[pending.action] : null;
  const subLine = (row: AdminUser) =>
    [
      vi.roles[row.role] ?? row.role,
      row.storeId == null ? null : t.storeNumber(row.storeId),
      formatDate(row.createdAt, ZONE),
    ]
      .filter(Boolean)
      .join(' · ');

  const columns: DataTableColumn<AdminUser>[] = [
    {
      key: 'email',
      header: t.email,
      className: 'pw-admin-col-name',
      render: (row) => (
        <>
          <span className="pw-cell-title" title={row.email}>
            {row.email}
          </span>
          {/* A narrow table: role, store and date fold under the email (G2.11). */}
          <span className="pw-cell-sub pw-admin-users-fold">{subLine(row)}</span>
        </>
      ),
    },
    {
      key: 'role',
      header: t.role,
      className: 'pw-nowrap pw-admin-users-col-fold',
      render: (row) => vi.roles[row.role] ?? row.role,
    },
    {
      key: 'store',
      header: t.store,
      className: 'pw-nowrap pw-admin-users-col-fold',
      render: (row) =>
        row.storeId == null ? (
          '—'
        ) : (
          <Link to={`/admin/stores/${row.storeId}`} onClick={(event) => event.stopPropagation()}>
            {t.storeNumber(row.storeId)}
          </Link>
        ),
    },
    { key: 'status', header: t.status, render: (row) => <StatusPill kind="member" status={row.status} /> },
    {
      key: 'created',
      header: t.created,
      numeric: true,
      className: 'pw-nowrap pw-admin-users-col-fold',
      render: (row) => formatDate(row.createdAt, ZONE),
    },
    {
      key: 'actions',
      header: <span className="pw-visually-hidden">{t.actions}</span>,
      align: 'end',
      render: menuButton,
    },
  ];

  return (
    <section className="pw-page-section" aria-labelledby="users-title">
      <div className="pw-admin-intro">
        <h1 id="users-title" className="pw-page-title">
          {t.title}
        </h1>
        <p className="pw-muted">{t.lead}</p>
      </div>

      <div ref={filters} className="pw-list-filters" role="group" aria-label={t.filterLabel}>
        {storeId ? (
          <PresetFilter
            label={t.store}
            value={storeName || t.storeNumber(storeId)}
            clearText={t.clearStore}
            onClear={() => update({ storeId: undefined, storeName: undefined })}
          />
        ) : null}
        <PopUpFilter
          label={t.role}
          value={role}
          emptyText={t.all}
          options={ROLES.map((value) => ({ value, label: vi.roles[value] ?? value }))}
          onChange={(value) => update({ role: value })}
        />
        <PopUpFilter
          label={t.status}
          value={status}
          emptyText={t.all}
          options={[
            { value: 'ACTIVE', label: t.statusActive },
            { value: 'PENDING_VERIFICATION', label: t.statusPending },
            { value: 'DISABLED', label: t.statusDisabled },
          ]}
          onChange={(value) => update({ status: value })}
        />
        {clearPresence !== 'closed' ? (
          <ClearFilters
            text={vi.plans.clearFilters}
            closing={clearPresence === 'closing'}
            onClear={() => update({ storeId: undefined, storeName: undefined, role: undefined, status: undefined })}
          />
        ) : null}
      </div>

      {users.isError ? <ErrorBanner title={messageFor(users.error)} /> : null}

      <div ref={probe} className="pw-fit-probe" aria-hidden="true" inert>
        {wide ? (
          <>
            <div className="pw-list-group pw-table-wrap pw-admin-users-table">
              <table className="pw-table">
                <thead>
                  <tr>
                    <th>{t.email}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="pw-admin-col-name">
                      <span className="pw-cell-title">
                        {t.email}
                        <br />
                        {t.email}
                      </span>
                      <span className="pw-cell-sub pw-admin-users-fold">{t.email}</span>
                    </td>
                    <td>
                      <button type="button" className="pw-icon-button" tabIndex={-1}>
                        <EllipsisIcon />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <Pager label={t.pagerLabel} page={0} totalPages={2} onPage={() => {}} />
          </>
        ) : null}
      </div>
      {!wide && data !== undefined && !empty ? (
        <Pager label={t.pagerLabel} page={page} totalPages={totalPages} onPage={goToPage} pinned />
      ) : null}
      <div
        ref={result}
        key={shown.key}
        className={cx('pw-list-result', shown.motion, users.isPlaceholderData && 'is-stale')}
        style={wide && fit && !empty ? { minHeight: fit.height } : undefined}
        aria-busy={users.isPlaceholderData || undefined}
      >
        {loading ? (
          <div className="pw-list-group">
            <PlaceholderRows rows={Math.min(size ?? 6, 12)} label={t.loading} />
          </div>
        ) : empty ? (
          <section className="pw-list-empty" aria-labelledby="users-empty-title">
            <span className="pw-list-empty-symbol" aria-hidden="true">
              <UsersIcon />
            </span>
            <h2 id="users-empty-title">{t.empty}</h2>
            {filtered ? <p>{t.emptyHint}</p> : null}
          </section>
        ) : wide ? (
          <DataTable<AdminUser>
            caption={t.caption}
            columns={columns}
            rows={items}
            resizable
            className="pw-list-group pw-admin-users-table"
            onRowContextMenu={(row, point) => rowMenu.openAt(row, point)}
          />
        ) : (
          <ul className="pw-list-group pw-list-rows" aria-label={t.caption}>
            {items.map((row) => (
              <li
                key={row.id}
                className={cx('pw-list-row pw-admin-row', rowMenu.menu?.row.id === row.id && 'is-held')}
                {...rowMenu.holdHandlers(row)}
              >
                {/* The status under the text, so the email keeps the row's width (G2.11, narrow lists). */}
                <span className="pw-list-row-main">
                  <span className="pw-admin-row-title">{row.email}</span>
                  <span className="pw-list-row-sub">{subLine(row)}</span>
                  <span>
                    <StatusPill kind="member" status={row.status} />
                  </span>
                </span>
                {menuButton(row)}
              </li>
            ))}
          </ul>
        )}
      </div>
      {wide && data !== undefined && !empty ? (
        <Pager label={t.pagerLabel} page={page} totalPages={totalPages} onPage={goToPage} />
      ) : null}

      {rowMenu.menu ? (
        <RowMenu
          rect={rowMenu.menu.rect}
          label={rowMenu.menu.row.email}
          onClose={rowMenu.close}
          items={menuItems(rowMenu.menu.row)}
        />
      ) : null}
      <Alert
        open={pending !== null}
        title={dialog?.[0] ?? ''}
        message={failure ? messageFor(failure) : dialog && pending ? dialog[2].replace('{e}', pending.user.email) : ''}
        actionLabel={dialog?.[1] ?? ''}
        destructive={pending?.action === 'disable'}
        busy={busy}
        onCancel={() => setPending(null)}
        onAction={() => void confirm()}
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

/** The platform log filtered to what this person did (G7-Q2: set from here, with their email to show). */
function auditLink(row: AdminUser): string {
  return `/admin/audit?${new URLSearchParams({ actorId: String(row.id), actor: row.email })}`;
}
