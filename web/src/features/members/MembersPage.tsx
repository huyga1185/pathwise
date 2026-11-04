import { useRef, useState, type KeyboardEvent } from 'react';
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
import { vi } from '@/shared/i18n/vi';
import {
  Button,
  Checkbox,
  DataTable,
  Dialog,
  ErrorBanner,
  SelectField,
  StatusPill,
  TextField,
  type DataTableColumn,
} from '@/shared/ui';
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

type Tab = 'members' | 'invitations';

/**
 * Prototype "Thành viên và lời mời" (SCREENS §1.1). The OWNER changes roles, "Đi giao" and status of the others (only
 * "Đi giao" of themselves) and manages invitations; a DISPATCHER sees the members read-only and no invitations.
 */
export function MembersPage() {
  const { session } = useSession();
  const me = session.status === 'authenticated' ? session.user : null;
  const isOwner = me?.role === 'OWNER';
  const [params, setParams] = useSearchParams();
  const tab: Tab = isOwner && params.get('tab') === 'invitations' ? 'invitations' : 'members';
  const members = useMembers();
  const invitations = useInvitations(isOwner);
  const store = useStoreQuery();
  const timeZone = store.data?.timezone ?? DEVICE_TIME_ZONE;
  const tabRefs = useRef<Record<Tab, HTMLButtonElement | null>>({ members: null, invitations: null });

  const select = (next: Tab) => {
    setParams(next === 'invitations' ? { tab: 'invitations' } : {}, { replace: true });
    tabRefs.current[next]?.focus();
  };
  const onTabKey = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (!isOwner || (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft')) return;
    event.preventDefault();
    select(tab === 'members' ? 'invitations' : 'members');
  };

  return (
    <section className="pw-page-section" aria-labelledby="members-title">
      <div className="pw-members-intro">
        <h1 id="members-title" className="pw-page-title">
          {vi.members.title}
        </h1>
        <p className="pw-muted">{isOwner ? vi.members.leadOwner : vi.members.leadDispatcher}</p>
      </div>

      <div className="pw-tabs" role="tablist" aria-label={vi.members.tabsLabel}>
        <button
          ref={(element) => {
            tabRefs.current.members = element;
          }}
          type="button"
          role="tab"
          id="tab-members"
          className="pw-tab"
          aria-selected={tab === 'members'}
          aria-controls="panel-members"
          tabIndex={tab === 'members' ? 0 : -1}
          onClick={() => select('members')}
          onKeyDown={onTabKey}
        >
          {vi.members.tabMembers(members.data?.length ?? 0)}
        </button>
        {isOwner ? (
          <button
            ref={(element) => {
              tabRefs.current.invitations = element;
            }}
            type="button"
            role="tab"
            id="tab-invitations"
            className="pw-tab"
            aria-selected={tab === 'invitations'}
            aria-controls="panel-invitations"
            tabIndex={tab === 'invitations' ? 0 : -1}
            onClick={() => select('invitations')}
            onKeyDown={onTabKey}
          >
            {vi.members.tabInvitations(invitations.data?.length ?? 0)}
          </button>
        ) : null}
      </div>

      {tab === 'members' ? (
        <div id="panel-members" role="tabpanel" aria-labelledby="tab-members" className="pw-members-panel">
          {members.isError ? <ErrorBanner title={messageFor(members.error)} /> : null}
          <MembersTable
            members={members.data ?? []}
            loading={members.isPending}
            meId={me?.id ?? null}
            isOwner={isOwner}
          />
          <p id="drive-hint" className="pw-muted">
            {vi.members.driveHint}
          </p>
        </div>
      ) : (
        <div id="panel-invitations" role="tabpanel" aria-labelledby="tab-invitations" className="pw-members-invite">
          <InviteForm />
          <section className="pw-members-invitations" aria-label={vi.members.sentLabel}>
            {invitations.isError ? <ErrorBanner title={messageFor(invitations.error)} /> : null}
            <InvitationsTable
              invitations={invitations.data ?? []}
              loading={invitations.isPending}
              timeZone={timeZone}
            />
          </section>
        </div>
      )}
    </section>
  );
}

function memberName(member: Member, meId: number | null) {
  return `${member.fullName ?? vi.members.noName}${member.id === meId ? vi.members.you : ''}`;
}

function MembersTable({
  members,
  loading,
  meId,
  isOwner,
}: {
  members: Member[];
  loading: boolean;
  meId: number | null;
  isOwner: boolean;
}) {
  const memberApi = useMemberApi();
  const [pending, setPending] = useState<number | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [confirm, setConfirm] = useState<Member | null>(null);
  const [dialogFailure, setDialogFailure] = useState<unknown>(null);

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

  const disable = async () => {
    if (!confirm) return;
    setDialogFailure(null);
    setPending(confirm.id);
    try {
      await memberApi.updateMember(confirm.id, { status: 'DISABLED' });
      setConfirm(null);
    } catch (error) {
      setDialogFailure(error);
    } finally {
      setPending(null);
    }
  };

  const columns: DataTableColumn<Member>[] = [
    {
      key: 'member',
      header: vi.members.member,
      render: (row) => (
        <div>
          <div className="pw-members-name">{memberName(row, meId)}</div>
          <div className="pw-members-email">{row.email}</div>
        </div>
      ),
    },
    {
      key: 'role',
      header: vi.members.role,
      render: (row) =>
        isOwner && row.role !== 'OWNER' ? (
          <select
            className="pw-field-input pw-members-role"
            aria-label={vi.members.roleOf(memberName(row, meId))}
            value={row.role}
            disabled={pending === row.id}
            onChange={(event) =>
              void act(row, () => memberApi.updateMember(row.id, { role: event.target.value as InvitableRole }))
            }
          >
            {ROLE_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        ) : (
          (vi.roles[row.role] ?? row.role)
        ),
    },
    { key: 'status', header: vi.members.status, render: (row) => <StatusPill kind="member" status={row.status} /> },
    {
      key: 'drive',
      header: vi.members.drive,
      render: (row) => {
        // A DRIVER always drives (D-21); only the OWNER changes it, their own included.
        const locked = !isOwner || row.role === 'DRIVER' || pending === row.id;
        return (
          <Checkbox
            label={row.canDrive ? vi.members.yes : vi.members.no}
            aria-label={vi.members.driveOf(memberName(row, meId))}
            aria-describedby="drive-hint"
            checked={row.canDrive}
            disabled={locked}
            onChange={(event) => {
              const canDrive = event.target.checked;
              void act(row, () =>
                row.role === 'OWNER'
                  ? memberApi.updateOwnCanDrive(canDrive)
                  : memberApi.updateMember(row.id, { canDrive }),
              );
            }}
          />
        );
      },
    },
    {
      key: 'actions',
      header: <span className="pw-visually-hidden">{vi.members.actions}</span>,
      align: 'end',
      render: (row) => {
        if (!isOwner || row.role === 'OWNER') return null;
        const name = memberName(row, meId);
        return row.status === 'DISABLED' ? (
          <Button
            variant="quiet"
            busy={pending === row.id}
            aria-label={vi.members.enableLabel(name)}
            onClick={() => void act(row, () => memberApi.updateMember(row.id, { status: 'ACTIVE' }))}
          >
            {vi.members.enable}
          </Button>
        ) : (
          <Button
            variant="quiet"
            aria-label={vi.members.disableLabel(name)}
            onClick={() => {
              setDialogFailure(null);
              setConfirm(row);
            }}
          >
            {vi.members.disable}
          </Button>
        );
      },
    },
  ];

  return (
    <>
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <DataTable
        caption={vi.members.caption}
        columns={columns}
        rows={members}
        className="pw-members-table"
        empty={loading ? vi.pages.loading : undefined}
      />
      <Dialog
        open={confirm !== null}
        tone="danger"
        title={confirm ? vi.members.disableTitle(memberName(confirm, meId)) : ''}
        confirmLabel={vi.members.disable}
        cancelLabel={vi.members.back}
        busy={confirm !== null && pending === confirm.id}
        onConfirm={() => void disable()}
        onClose={() => setConfirm(null)}
      >
        {dialogFailure ? <ErrorBanner title={messageFor(dialogFailure)} /> : null}
        {vi.members.disableBody.map((line) => (
          <p key={line}>{line}</p>
        ))}
      </Dialog>
    </>
  );
}

interface InviteFormValues {
  email: string;
  role: InvitableRole;
}

function InviteForm() {
  const memberApi = useMemberApi();
  const lock = useCountdown();
  const [sent, setSent] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<InviteFormValues>({ defaultValues: { email: '', role: 'DRIVER' } });

  const onInvite = handleSubmit(async (form) => {
    setSent(null);
    setFailure(null);
    const email = form.email.trim();
    try {
      await memberApi.invite({ email, role: form.role });
      setSent(vi.members.invited(email));
      reset({ email: '', role: form.role });
    } catch (error) {
      if (errorCode(error) === 'EMAIL_EXISTED') {
        setError('email', { type: 'server', message: errorMessages.EMAIL_EXISTED }, { shouldFocus: true });
      } else if (
        !applyServerErrors(error, setError, ['email', 'role'], { email: vi.members.email, role: vi.members.role })
      ) {
        if (error instanceof ApiError && error.code === 'RATE_LIMITED' && error.retryAfter !== null) {
          lock.start(error.retryAfter);
        }
        setFailure(error);
      }
    }
  });

  return (
    <form className="pw-card pw-members-invite-form" aria-labelledby="inv-title" onSubmit={onInvite} noValidate>
      <h2 id="inv-title">{vi.members.inviteTitle}</h2>
      {sent ? (
        <div role="status" className="pw-status-ok">
          {sent}
        </div>
      ) : null}
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <TextField
        label={vi.members.email}
        type="email"
        inputMode="email"
        autoComplete="off"
        required
        placeholder="ten@example.com"
        error={errors.email?.message}
        {...register('email', emailRules(vi.members.email))}
      />
      <SelectField
        label={vi.members.role}
        required
        options={ROLE_OPTIONS}
        error={errors.role?.message}
        {...register('role')}
      />
      <p className="pw-muted">{vi.members.inviteNote}</p>
      <div>
        <Button type="submit" variant="primary" busy={isSubmitting} disabled={lock.remaining > 0}>
          {lock.remaining > 0 ? vi.auth.retryIn(formatWait(lock.remaining)) : vi.members.inviteSubmit}
        </Button>
      </div>
    </form>
  );
}

function InvitationsTable({
  invitations,
  loading,
  timeZone,
}: {
  invitations: Invitation[];
  loading: boolean;
  timeZone: string;
}) {
  const memberApi = useMemberApi();
  const [pending, setPending] = useState<number | null>(null);
  const [failure, setFailure] = useState<unknown>(null);

  const revoke = async (invitation: Invitation) => {
    setFailure(null);
    setPending(invitation.id);
    try {
      await memberApi.revokeInvitation(invitation.id);
    } catch (error) {
      setFailure(error);
    } finally {
      setPending(null);
    }
  };

  const columns: DataTableColumn<Invitation>[] = [
    { key: 'email', header: vi.members.email, render: (row) => row.email },
    { key: 'role', header: vi.members.role, render: (row) => vi.roles[row.role] ?? row.role },
    { key: 'status', header: vi.members.status, render: (row) => <StatusPill kind="invitation" status={row.status} /> },
    {
      key: 'expires',
      header: vi.members.expires,
      render: (row) => <span className="pw-members-date">{formatDate(row.expiresAt, timeZone)}</span>,
    },
    {
      key: 'actions',
      header: <span className="pw-visually-hidden">{vi.members.actions}</span>,
      align: 'end',
      render: (row) =>
        row.status === 'PENDING' ? (
          <Button
            variant="quiet"
            busy={pending === row.id}
            aria-label={vi.members.revokeLabel(row.email)}
            onClick={() => void revoke(row)}
          >
            {vi.members.revoke}
          </Button>
        ) : null,
    },
  ];

  return (
    <>
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <DataTable
        caption={vi.members.invitationsCaption}
        columns={columns}
        rows={invitations}
        className="pw-members-invitations-table"
        empty={loading ? vi.pages.loading : vi.members.noInvitations}
      />
    </>
  );
}
