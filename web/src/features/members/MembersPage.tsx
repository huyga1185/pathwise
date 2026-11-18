import { useStoreReadOnly } from '@/data/meQuery';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useSearchParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { useStoreQuery } from '@/data/storeQuery';
import { useSession } from '@/features/auth';
import { errorMessages, formatWait, messageFor } from '@/shared/errors';
import { emailRules } from '@/shared/forms/rules';
import { applyServerErrors, errorCode } from '@/shared/forms/serverErrors';
import { formatDate } from '@/shared/format';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import {
  Alert,
  Button,
  buttonClass,
  Checkbox,
  ChevronRightIcon,
  ChevronUpDownIcon,
  DataTable,
  ErrorBanner,
  PlaceholderRows,
  PopUpButton,
  RowMenu,
  SegmentedControl,
  SelectField,
  Sheet,
  StatusPill,
  SwitchRow,
  TextField,
  Toast,
  ToastDock,
  UserPlusIcon,
  useRowMenu,
  type DataTableColumn,
} from '@/shared/ui';
import { cx } from '@/shared/ui/cx';
import {
  useInvitations,
  useMemberApi,
  useMembers,
  type InvitableRole,
  type Invitation,
  type Member,
} from './memberApi';
import './members.css';

const ROLE_OPTIONS = [
  { value: 'DRIVER', label: vi.roles.DRIVER ?? 'DRIVER' },
  { value: 'DISPATCHER', label: vi.roles.DISPATCHER ?? 'DISPATCHER' },
] as const;
const DEVICE_TIME_ZONE = Intl.DateTimeFormat().resolvedOptions().timeZone;
const m = vi.members;

type Tab = 'members' | 'invitations';

function memberName(member: Member, meId: number | null) {
  return `${member.fullName ?? m.noName}${member.id === meId ? m.you : ''}`;
}

/**
 * "Thành viên và lời mời" (SCREENS §1.1, OWNER_UI_REDESIGN §8.3). The OWNER invites from the title row (a sheet),
 * changes roles, "Đi giao" and status of the others (only "Đi giao" of themselves) and manages invitations; a
 * DISPATCHER sees the members read-only and no invitations. Members / invitations: a tab view on a wide window, a
 * segmented control on a phone (owner's choice Q6). Wide: tables with the role as a pop-up button in the row; phone:
 * rows that open a sheet with the role, an "Đi giao" switch and "Vô hiệu hoá" (HIG Toggles: a switch only in a list
 * row). Disabling asks first in an alert (people are signed out at once). Touch and hold or right click a row for its
 * commands.
 */
export function MembersPage() {
  const { session } = useSession();
  const me = session.status === 'authenticated' ? session.user : null;
  const isOwner = me?.role === 'OWNER';
  const { readOnly } = useStoreReadOnly();
  const wide = useMediaQuery(WIDE_QUERY);
  const [params, setParams] = useSearchParams();
  const tab: Tab = isOwner && params.get('tab') === 'invitations' ? 'invitations' : 'members';
  const members = useMembers();
  const invitations = useInvitations(isOwner);
  const store = useStoreQuery();
  const timeZone = store.data?.timezone ?? DEVICE_TIME_ZONE;
  const [inviting, setInviting] = useState(false);
  const [toast, setToast] = useState<string | null>(null);

  const select = (next: Tab) => setParams(next === 'invitations' ? { tab: 'invitations' } : {}, { replace: true });
  const tabs = [
    { value: 'members' as const, label: m.tabMembers(members.data?.length ?? 0), panelId: 'panel-members' },
    ...(isOwner
      ? [
          {
            value: 'invitations' as const,
            label: m.tabInvitations(invitations.data?.length ?? 0),
            panelId: 'panel-invitations',
          },
        ]
      : []),
  ];

  return (
    <section className="pw-page-section" aria-labelledby="members-title">
      <div className="pw-list-head">
        <h1 id="members-title" className="pw-page-title">
          {m.title}
        </h1>
        {isOwner && !readOnly ? (
          <button
            type="button"
            className={wide ? buttonClass('primary') : 'pw-icon-button pw-list-add'}
            aria-label={wide ? undefined : m.inviteTitle}
            title={wide ? undefined : m.inviteTitle}
            onClick={() => setInviting(true)}
          >
            <UserPlusIcon />
            {wide ? m.inviteShort : null}
          </button>
        ) : null}
      </div>
      <p className="pw-muted pw-list-lead">{isOwner ? m.leadOwner : m.leadDispatcher}</p>

      {tabs.length > 1 ? (
        wide ? (
          <div className="pw-tabs" role="tablist" aria-label={m.tabsLabel}>
            {tabs.map((item) => (
              <button
                key={item.value}
                type="button"
                role="tab"
                id={`tab-${item.value}`}
                className="pw-tab"
                aria-selected={tab === item.value}
                aria-controls={item.panelId}
                tabIndex={tab === item.value ? 0 : -1}
                onClick={() => select(item.value)}
                onKeyDown={(event) => {
                  if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
                  event.preventDefault();
                  const next = tab === 'members' ? 'invitations' : 'members';
                  select(next);
                  document.getElementById(`tab-${next}`)?.focus();
                }}
              >
                {item.label}
              </button>
            ))}
          </div>
        ) : (
          <SegmentedControl label={m.tabsLabel} value={tab} options={tabs} onChange={select} idPrefix="tab" />
        )
      ) : null}

      {tab === 'members' ? (
        <div id="panel-members" role="tabpanel" aria-labelledby="tab-members" className="pw-members-panel">
          {members.isError ? <ErrorBanner title={messageFor(members.error)} /> : null}
          <MembersList
            members={members.data ?? []}
            loading={members.isPending}
            meId={me?.id ?? null}
            isOwner={isOwner && !readOnly}
            wide={wide}
            onDone={setToast}
          />
          <p id="drive-hint" className="pw-muted">
            {m.driveHint}
          </p>
        </div>
      ) : (
        <div id="panel-invitations" role="tabpanel" aria-labelledby="tab-invitations" className="pw-members-panel">
          {invitations.isError ? <ErrorBanner title={messageFor(invitations.error)} /> : null}
          <InvitationsList
            invitations={invitations.data ?? []}
            loading={invitations.isPending}
            timeZone={timeZone}
            wide={wide}
            onDone={setToast}
          />
        </div>
      )}

      <InviteSheet
        open={inviting}
        onClose={() => setInviting(false)}
        onSent={(email) => {
          setInviting(false);
          setToast(m.invited(email));
          select('invitations');
        }}
      />
      {toast ? (
        <ToastDock>
          <Toast tone="success" onClose={() => setToast(null)}>
            {toast}
          </Toast>
        </ToastDock>
      ) : null}
    </section>
  );
}

function MembersList({
  members,
  loading,
  meId,
  isOwner,
  wide,
  onDone,
}: {
  members: Member[];
  loading: boolean;
  meId: number | null;
  isOwner: boolean;
  wide: boolean;
  onDone: (message: string) => void;
}) {
  const memberApi = useMemberApi();
  const rowMenu = useRowMenu<Member>();
  const [pending, setPending] = useState<number | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [confirm, setConfirm] = useState<Member | null>(null);
  // Phone: the member whose sheet is open (its data read again from the list as it changes).
  const [openId, setOpenId] = useState<number | null>(null);
  const opened = members.find((member) => member.id === openId) ?? null;

  // A change waits for the server, then the row shows it; an error goes to the top of the list.
  const act = async (member: Member, action: () => Promise<void>) => {
    setFailure(null);
    setPending(member.id);
    try {
      await action();
    } catch (error) {
      setFailure(error);
    } finally {
      setPending(null);
    }
  };
  const setRole = (member: Member, role: InvitableRole) =>
    void act(member, () => memberApi.updateMember(member.id, { role }));
  // A DRIVER always drives (D-21); only the OWNER changes it, their own included.
  const setDrive = (member: Member, canDrive: boolean) =>
    void act(member, () =>
      member.role === 'OWNER' ? memberApi.updateOwnCanDrive(canDrive) : memberApi.updateMember(member.id, { canDrive }),
    );
  const enable = (member: Member) =>
    void act(member, async () => {
      await memberApi.updateMember(member.id, { status: 'ACTIVE' });
      onDone(m.enabled(memberName(member, meId)));
    });
  const disable = async () => {
    if (!confirm) return;
    const member = confirm;
    setConfirm(null);
    await act(member, async () => {
      await memberApi.updateMember(member.id, { status: 'DISABLED' });
      onDone(m.disabled(memberName(member, meId)));
    });
  };
  const manageable = (member: Member) => isOwner && member.role !== 'OWNER';
  const menuItems = (member: Member) => [
    ...(manageable(member)
      ? [
          {
            label: member.role === 'DRIVER' ? m.makeDispatcher : m.makeDriver,
            onSelect: () => setRole(member, member.role === 'DRIVER' ? 'DISPATCHER' : 'DRIVER'),
          },
        ]
      : []),
    ...(isOwner && member.role !== 'DRIVER'
      ? [{ label: member.canDrive ? m.driveOff : m.driveOn, onSelect: () => setDrive(member, !member.canDrive) }]
      : []),
    ...(manageable(member)
      ? member.status === 'DISABLED'
        ? [{ label: m.enable, onSelect: () => enable(member) }]
        : [{ label: m.disableMenu, destructive: true, onSelect: () => setConfirm(member) }]
      : []),
  ];

  const columns: DataTableColumn<Member>[] = [
    {
      key: 'member',
      header: m.member,
      render: (row) => (
        <div>
          <div className="pw-members-name">{memberName(row, meId)}</div>
          <div className="pw-members-email">{row.email}</div>
        </div>
      ),
    },
    {
      key: 'role',
      header: m.role,
      render: (row) =>
        manageable(row) ? (
          <PopUpButton
            menuLabel={m.roleOf(memberName(row, meId))}
            buttonLabel={`${m.roleOf(memberName(row, meId))}: ${vi.roles[row.role] ?? row.role}`}
            className="pw-inline-popup"
            value={row.role}
            choices={ROLE_OPTIONS}
            onChange={(role) => setRole(row, role as InvitableRole)}
          >
            {vi.roles[row.role] ?? row.role}
            <ChevronUpDownIcon />
          </PopUpButton>
        ) : (
          (vi.roles[row.role] ?? row.role)
        ),
    },
    { key: 'status', header: m.status, render: (row) => <StatusPill kind="member" status={row.status} /> },
    {
      key: 'drive',
      header: m.drive,
      render: (row) => (
        // No "Có/Không" beside it: the tick shows the state and the heading "Đi giao" what it is (HIG Toggles); a
        // label that changed with the state also made the column change width.
        <Checkbox
          aria-label={m.driveOf(memberName(row, meId))}
          aria-describedby="drive-hint"
          checked={row.canDrive}
          disabled={!isOwner || row.role === 'DRIVER' || pending === row.id}
          onChange={(event) => setDrive(row, event.target.checked)}
        />
      ),
    },
    {
      key: 'actions',
      header: <span className="pw-visually-hidden">{m.actions}</span>,
      align: 'end',
      render: (row) => {
        if (!manageable(row)) return null;
        const name = memberName(row, meId);
        return row.status === 'DISABLED' ? (
          <Button
            variant="quiet"
            busy={pending === row.id}
            aria-label={m.enableLabel(name)}
            onClick={() => enable(row)}
          >
            {m.enable}
          </Button>
        ) : (
          <Button variant="quiet" aria-label={m.disableLabel(name)} onClick={() => setConfirm(row)}>
            {m.disable}
          </Button>
        );
      },
    },
  ];

  return (
    <>
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      {loading ? (
        <div className="pw-list-group">
          <PlaceholderRows rows={4} label={m.loading} />
        </div>
      ) : wide ? (
        <DataTable
          caption={m.caption}
          columns={columns}
          rows={members}
          resizable
          className="pw-list-group"
          onRowContextMenu={
            isOwner ? (row, point) => menuItems(row).length > 0 && rowMenu.openAt(row, point) : undefined
          }
        />
      ) : (
        <ul className="pw-list-group pw-list-rows" aria-label={m.caption}>
          {members.map((row) => {
            const name = memberName(row, meId);
            return (
              <li
                key={row.id}
                className={cx('pw-list-row', rowMenu.menu?.row.id === row.id && 'is-held')}
                {...(isOwner && menuItems(row).length > 0 ? rowMenu.holdHandlers(row) : {})}
              >
                <button
                  type="button"
                  className="pw-list-row-main"
                  disabled={!isOwner}
                  aria-label={isOwner ? m.openLabel(name) : undefined}
                  onClick={(event) => {
                    rowMenu.consumeHold(event);
                    if (!event.defaultPrevented) setOpenId(row.id);
                  }}
                >
                  <span className="pw-list-row-title">{name}</span>
                  {/* The role, then the email cut with "…" rather than broken mid-word (the sheet shows it whole). */}
                  <span className="pw-list-row-sub pw-members-sub">
                    <span>{vi.roles[row.role] ?? row.role}</span>
                    <span className="pw-members-sub-email">{row.email}</span>
                  </span>
                </button>
                <StatusPill kind="member" status={row.status} />
                {isOwner ? (
                  <span className="pw-list-row-chevron" aria-hidden="true">
                    <ChevronRightIcon />
                  </span>
                ) : null}
              </li>
            );
          })}
        </ul>
      )}

      {rowMenu.menu ? (
        <RowMenu
          rect={rowMenu.menu.rect}
          label={memberName(rowMenu.menu.row, meId)}
          onClose={rowMenu.close}
          items={menuItems(rowMenu.menu.row)}
        />
      ) : null}

      <Sheet
        open={opened !== null}
        title={opened ? memberName(opened, meId) : ''}
        cancelLabel={m.close}
        onClose={() => setOpenId(null)}
      >
        {opened ? (
          <>
            <p className="pw-muted pw-members-sheet-email">{opened.email}</p>
            {manageable(opened) ? (
              <SelectField
                label={m.role}
                options={ROLE_OPTIONS}
                value={opened.role}
                disabled={pending === opened.id}
                onChange={(event) => setRole(opened, event.target.value as InvitableRole)}
              />
            ) : (
              <p>
                <strong>{m.role}:</strong> {vi.roles[opened.role] ?? opened.role}
              </p>
            )}
            <div className="pw-members-sheet-group">
              <SwitchRow
                label={m.drive}
                hint={opened.role === 'DRIVER' ? m.driverAlwaysDrives : undefined}
                checked={opened.canDrive}
                disabled={opened.role === 'DRIVER' || pending === opened.id}
                onChange={(canDrive) => setDrive(opened, canDrive)}
              />
            </div>
            <p className="pw-muted">{m.driveHint}</p>
            {manageable(opened) ? (
              opened.status === 'DISABLED' ? (
                <Button variant="secondary" busy={pending === opened.id} onClick={() => enable(opened)}>
                  {m.enable}
                </Button>
              ) : (
                <Button
                  variant="secondary"
                  className="pw-members-disable"
                  onClick={() => {
                    setOpenId(null);
                    setConfirm(opened);
                  }}
                >
                  {m.disable}
                </Button>
              )
            ) : null}
          </>
        ) : null}
      </Sheet>

      <Alert
        open={confirm !== null}
        title={confirm ? m.disableTitle(memberName(confirm, meId)) : ''}
        message={m.disableMessage}
        actionLabel={m.disable}
        busy={confirm !== null && pending === confirm.id}
        onAction={() => void disable()}
        onCancel={() => setConfirm(null)}
      />
    </>
  );
}

interface InviteFormValues {
  email: string;
  role: InvitableRole;
}

/** Inviting someone: a sheet with the email and the role (HIG Sheets); "Gửi" sends. */
function InviteSheet({
  open,
  onClose,
  onSent,
}: {
  open: boolean;
  onClose: () => void;
  onSent: (email: string) => void;
}) {
  const memberApi = useMemberApi();
  const lock = useCountdown();
  const [failure, setFailure] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<InviteFormValues>({ defaultValues: { email: '', role: 'DRIVER' } });
  const [shown, setShown] = useState(open);
  if (open !== shown) {
    setShown(open);
    if (open) {
      setFailure(null);
      reset({ email: '', role: 'DRIVER' });
    }
  }

  const onInvite = handleSubmit(async (form) => {
    setFailure(null);
    const email = form.email.trim();
    try {
      await memberApi.invite({ email, role: form.role });
      onSent(email);
    } catch (error) {
      if (errorCode(error) === 'EMAIL_EXISTED') {
        setError('email', { type: 'server', message: errorMessages.EMAIL_EXISTED }, { shouldFocus: true });
      } else if (!applyServerErrors(error, setError, ['email', 'role'], { email: m.email, role: m.role })) {
        if (error instanceof ApiError && error.code === 'RATE_LIMITED' && error.retryAfter !== null) {
          lock.start(error.retryAfter);
        }
        setFailure(error);
      }
    }
  });

  return (
    <Sheet
      open={open}
      title={m.inviteTitle}
      doneLabel={lock.remaining > 0 ? vi.auth.retryIn(formatWait(lock.remaining)) : m.send}
      doneDisabled={lock.remaining > 0}
      busy={isSubmitting}
      dirty={isDirty}
      onSubmit={onInvite}
      onClose={onClose}
    >
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <TextField
        label={m.email}
        type="email"
        inputMode="email"
        autoComplete="off"
        required
        placeholder="ten@example.com"
        error={errors.email?.message}
        {...register('email', emailRules(m.email))}
      />
      <SelectField label={m.role} required options={ROLE_OPTIONS} error={errors.role?.message} {...register('role')} />
      <p className="pw-muted">{m.inviteNote}</p>
    </Sheet>
  );
}

function InvitationsList({
  invitations,
  loading,
  timeZone,
  wide,
  onDone,
}: {
  invitations: Invitation[];
  loading: boolean;
  timeZone: string;
  wide: boolean;
  onDone: (message: string) => void;
}) {
  const memberApi = useMemberApi();
  const { readOnly } = useStoreReadOnly();
  const rowMenu = useRowMenu<Invitation>();
  const [pending, setPending] = useState<number | null>(null);
  const [failure, setFailure] = useState<unknown>(null);

  // No alert: people can invite again (HIG Alerts: not "for common, undoable actions").
  const revoke = async (invitation: Invitation) => {
    setFailure(null);
    setPending(invitation.id);
    try {
      await memberApi.revokeInvitation(invitation.id);
      onDone(m.revoked(invitation.email));
    } catch (error) {
      setFailure(error);
    } finally {
      setPending(null);
    }
  };
  const canRevoke = (invitation: Invitation) => !readOnly && invitation.status === 'PENDING';
  const revokeButton = (row: Invitation) =>
    canRevoke(row) ? (
      <Button
        variant="quiet"
        busy={pending === row.id}
        aria-label={m.revokeLabel(row.email)}
        onClick={() => void revoke(row)}
      >
        {m.revoke}
      </Button>
    ) : null;

  const columns: DataTableColumn<Invitation>[] = [
    { key: 'email', header: m.email, render: (row) => row.email },
    { key: 'role', header: m.role, render: (row) => vi.roles[row.role] ?? row.role },
    { key: 'status', header: m.status, render: (row) => <StatusPill kind="invitation" status={row.status} /> },
    {
      key: 'expires',
      header: m.expires,
      render: (row) => <span className="pw-members-date">{formatDate(row.expiresAt, timeZone)}</span>,
    },
    {
      key: 'actions',
      header: <span className="pw-visually-hidden">{m.actions}</span>,
      align: 'end',
      render: revokeButton,
    },
  ];

  if (loading) {
    return (
      <div className="pw-list-group">
        <PlaceholderRows rows={3} label={m.loadingInvitations} />
      </div>
    );
  }
  if (invitations.length === 0) return <p className="pw-muted">{m.noInvitations}</p>;
  return (
    <>
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      {wide ? (
        <DataTable
          caption={m.invitationsCaption}
          columns={columns}
          rows={invitations}
          resizable
          className="pw-list-group"
          onRowContextMenu={(row, point) => canRevoke(row) && rowMenu.openAt(row, point)}
        />
      ) : (
        <ul className="pw-list-group pw-list-rows" aria-label={m.invitationsCaption}>
          {invitations.map((row) => (
            <li
              key={row.id}
              className={cx('pw-list-row', rowMenu.menu?.row.id === row.id && 'is-held')}
              {...(canRevoke(row) ? rowMenu.holdHandlers(row) : {})}
            >
              {/* The status under the text, so the email has the room (HIG Typography: a stacked layout); an email
                  wraps only after its "@", never mid-word. */}
              <span className="pw-list-row-main">
                <span className="pw-list-row-title pw-email-title">
                  {row.email.split('@')[0]}@<wbr />
                  {row.email.split('@').slice(1).join('@')}
                </span>
                <span className="pw-list-row-sub">
                  {vi.roles[row.role] ?? row.role} · {m.expiresOn(formatDate(row.expiresAt, timeZone))}
                </span>
                <span className="pw-members-row-status">
                  <StatusPill kind="invitation" status={row.status} />
                </span>
              </span>
              {revokeButton(row)}
            </li>
          ))}
        </ul>
      )}
      {rowMenu.menu ? (
        <RowMenu
          rect={rowMenu.menu.rect}
          label={rowMenu.menu.row.email}
          onClose={rowMenu.close}
          items={[{ label: m.revokeMenu, destructive: true, onSelect: () => void revoke(rowMenu.menu!.row) }]}
        />
      ) : null}
    </>
  );
}
