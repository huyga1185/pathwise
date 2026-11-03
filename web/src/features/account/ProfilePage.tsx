import { useState, type FormEvent } from 'react';
import { useForm } from 'react-hook-form';
import { ApiError } from '@/data/apiError';
import { MAX_OTP_TRIES, useSession } from '@/features/auth';
import { errorMessages, fieldErrors, fieldMessage, formatWait, messageFor } from '@/shared/errors';
import { applyServerErrors, errorCode } from '@/shared/forms/serverErrors';
import { formatDayTime, formatPhone } from '@/shared/format';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { vi } from '@/shared/i18n/vi';
import { Button, ErrorBanner, StatusPill, TextField } from '@/shared/ui';
import { deviceName } from './device';
import { useDeviceSessions, useMe, useProfileApi, useStore, type DeviceSession, type Me } from './profileApi';
import './profile.css';

const DEVICE_TIME_ZONE = Intl.DateTimeFormat().resolvedOptions().timeZone;

/** Prototype "Hồ sơ và bảo mật" (SCREENS §1.1): profile, two-step password change, signed-in devices. */
export function ProfilePage() {
  const me = useMe();
  const store = useStore();
  const { logOut } = useSession();
  const timeZone = store.data?.timezone ?? DEVICE_TIME_ZONE;

  return (
    <section className="pw-profile" aria-labelledby="profile-title">
      <div className="pw-profile-head">
        <h1 id="profile-title" className="pw-page-title">
          {vi.profile.title}
        </h1>
        <Button variant="secondary" onClick={() => void logOut()}>
          {vi.profile.signOut}
        </Button>
      </div>
      {me.isError ? <ErrorBanner title={messageFor(me.error)} /> : null}
      <div className="pw-profile-cards">
        {me.data ? <ProfileCard me={me.data} /> : <section className="pw-profile-card" aria-busy="true" />}
        <PasswordCard email={me.data?.email ?? ''} />
      </div>
      <SessionsCard timeZone={timeZone} />
    </section>
  );
}

interface ProfileForm {
  fullName: string;
  phoneNumber: string;
}

const PROFILE_FIELDS = ['fullName', 'phoneNumber'] as const;
const PROFILE_LABELS = { fullName: vi.form.fullName, phoneNumber: vi.form.phone };

function ProfileCard({ me }: { me: Me }) {
  const profileApi = useProfileApi();
  const { updateUser } = useSession();
  const [saved, setSaved] = useState(false);
  const [failure, setFailure] = useState<unknown>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ProfileForm>({
    defaultValues: { fullName: me.fullName ?? '', phoneNumber: formatPhone(me.phoneNumber) },
  });

  const onSave = handleSubmit(async (form) => {
    setSaved(false);
    setFailure(null);
    try {
      // JSON Merge Patch (API_SPEC §1): an emptied field is sent as null to clear it (F-43: "" is not a phone number).
      const updated = await profileApi.updateMe({
        fullName: form.fullName.trim() || null,
        phoneNumber: form.phoneNumber.trim() || null,
      });
      updateUser({ fullName: updated.fullName ?? undefined });
      setSaved(true);
    } catch (error) {
      if (!applyServerErrors(error, setError, PROFILE_FIELDS, PROFILE_LABELS)) setFailure(error);
    }
  });

  return (
    <form className="pw-profile-card" aria-labelledby="p-title" onSubmit={onSave} noValidate>
      <h2 id="p-title">{vi.profile.info}</h2>
      {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
      <div className="pw-field">
        <span className="pw-field-label">{vi.profile.email}</span>
        <span>{me.email}</span>
        <p className="pw-field-msg">{vi.profile.emailHint}</p>
      </div>
      <div className="pw-field">
        <span className="pw-field-label">{vi.profile.role}</span>
        <span>{vi.roles[me.role] ?? me.role}</span>
      </div>
      <TextField
        label={vi.form.fullName}
        maxLength={100}
        autoComplete="name"
        placeholder={vi.profile.namePlaceholder}
        error={errors.fullName?.message}
        {...register('fullName', { onChange: () => setSaved(false) })}
      />
      <TextField
        label={vi.form.phone}
        type="tel"
        autoComplete="tel"
        placeholder="0905 123 456"
        hint={vi.form.phoneHint}
        error={errors.phoneNumber?.message}
        {...register('phoneNumber', { onChange: () => setSaved(false) })}
      />
      {saved ? (
        <p role="status" className="pw-profile-status">
          {vi.profile.saved}
        </p>
      ) : null}
      <div>
        <Button type="submit" variant="primary" busy={isSubmitting}>
          {vi.profile.save}
        </Button>
      </div>
    </form>
  );
}

type PasswordStep = 'current' | 'otp' | 'done';

function PasswordCard({ email }: { email: string }) {
  const profileApi = useProfileApi();
  const [step, setStep] = useState<PasswordStep>('current');
  const [current, setCurrent] = useState('');
  const [otp, setOtp] = useState('');
  const [password, setPassword] = useState('');
  const [errors, setErrors] = useState<Record<string, string | undefined>>({});
  const [alert, setAlert] = useState<string | null>(null);
  const [wrongTries, setWrongTries] = useState(0);
  const [busy, setBusy] = useState(false);
  const lock = useCountdown();

  const reset = () => {
    setStep('current');
    setCurrent('');
    setOtp('');
    setPassword('');
    setErrors({});
    setAlert(null);
    setWrongTries(0);
  };

  const onSendOtp = async (event: FormEvent) => {
    event.preventDefault();
    setAlert(null);
    if (!current) {
      setErrors({ current: fieldMessage('CURRENT_PASSWORD_REQUIRED', vi.profile.currentPassword) });
      return;
    }
    setBusy(true);
    try {
      await profileApi.requestPasswordOtp(current);
      setErrors({});
      setStep('otp');
    } catch (error) {
      const code = errorCode(error);
      if (code === 'INVALID_CURRENT_PASSWORD') setErrors({ current: errorMessages.INVALID_CURRENT_PASSWORD });
      else {
        if (error instanceof ApiError && code === 'RATE_LIMITED' && error.retryAfter !== null) {
          lock.start(error.retryAfter);
        }
        setAlert(messageFor(error));
      }
    } finally {
      setBusy(false);
    }
  };

  const onChange = async (event: FormEvent) => {
    event.preventDefault();
    setAlert(null);
    const next: Record<string, string | undefined> = {};
    if (otp.length !== 6) next.otp = fieldMessage('OTP_REQUIRED', vi.profile.otp);
    if (password.length < 8 || password.length > 72)
      next.password = fieldMessage('INVALID_PASSWORD_LENGTH', vi.form.newPassword);
    setErrors(next);
    if (next.otp || next.password || wrongTries >= MAX_OTP_TRIES) return;
    setBusy(true);
    try {
      await profileApi.changePassword({ otp, newPassword: password });
      setStep('done');
    } catch (error) {
      const code = errorCode(error);
      if (code === 'INVALID_OTP') {
        const tries = wrongTries + 1;
        setWrongTries(tries);
        setErrors({ otp: tries >= MAX_OTP_TRIES ? vi.form.otpTooManyTries : errorMessages.INVALID_OTP });
      } else {
        const fields = fieldErrors(error, { otp: vi.profile.otp, newPassword: vi.form.newPassword });
        if (code === 'VALIDATION_ERROR' && (fields.otp || fields.newPassword)) {
          setErrors({ otp: fields.otp, password: fields.newPassword });
        } else setAlert(messageFor(error));
      }
    } finally {
      setBusy(false);
    }
  };

  return (
    <form
      className="pw-profile-card"
      aria-labelledby="pw-title"
      onSubmit={step === 'current' ? (event) => void onSendOtp(event) : (event) => void onChange(event)}
      noValidate
    >
      <h2 id="pw-title">{vi.profile.password}</h2>
      {step === 'current' ? (
        <>
          <TextField
            label={vi.profile.currentPassword}
            type="password"
            autoComplete="current-password"
            required
            value={current}
            error={errors.current}
            onChange={(event) => setCurrent(event.target.value)}
          />
          {alert ? (
            <p role="alert" className="pw-profile-alert">
              {alert}
            </p>
          ) : null}
          <p className="pw-profile-muted">{vi.profile.otpNotice}</p>
          <div>
            <Button type="submit" variant="secondary" busy={busy} disabled={lock.remaining > 0}>
              {lock.remaining > 0 ? vi.auth.retryIn(formatWait(lock.remaining)) : vi.profile.sendOtp}
            </Button>
          </div>
        </>
      ) : null}
      {step === 'otp' ? (
        <>
          <p role="status" className="pw-profile-status">
            {vi.profile.otpSent(email)}
          </p>
          <TextField
            className="pw-field-code pw-field-code-sm"
            label={vi.profile.otp}
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            placeholder="••••••"
            value={otp}
            error={errors.otp}
            onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
          />
          <TextField
            label={vi.form.newPassword}
            type="password"
            autoComplete="new-password"
            required
            maxLength={72}
            hint={vi.form.passwordHint}
            value={password}
            error={errors.password}
            onChange={(event) => setPassword(event.target.value)}
          />
          {alert ? (
            <p role="alert" className="pw-profile-alert">
              {alert}
            </p>
          ) : null}
          <p className="pw-profile-muted">{vi.profile.changeNotice}</p>
          <div className="pw-profile-actions">
            <Button variant="quiet" onClick={reset}>
              {vi.profile.cancel}
            </Button>
            <Button type="submit" variant="primary" busy={busy} disabled={wrongTries >= MAX_OTP_TRIES}>
              {vi.profile.change}
            </Button>
          </div>
        </>
      ) : null}
      {step === 'done' ? (
        <>
          <div role="status" className="pw-profile-done">
            {vi.profile.changed}
          </div>
          <div>
            <Button variant="quiet" onClick={reset}>
              {vi.profile.done}
            </Button>
          </div>
        </>
      ) : null}
    </form>
  );
}

function SessionsCard({ timeZone }: { timeZone: string }) {
  const sessions = useDeviceSessions();
  const profileApi = useProfileApi();
  const { logOut } = useSession();
  const [failure, setFailure] = useState<unknown>(null);
  const [pending, setPending] = useState<string | null>(null);

  const list = [...(sessions.data ?? [])].sort(
    (a, b) => Number(b.current) - Number(a.current) || b.lastUsedAt.localeCompare(a.lastUsedAt),
  );
  const others = list.filter((item) => !item.current);

  const act = async (key: string, action: () => Promise<void>) => {
    setFailure(null);
    setPending(key);
    try {
      await action();
    } catch (error) {
      if (errorCode(error) !== 'NOT_FOUND') setFailure(error);
    } finally {
      setPending(null);
    }
  };

  return (
    <section className="pw-profile-card" aria-labelledby="s-title">
      <div className="pw-profile-sessions-head">
        <h2 id="s-title">{vi.profile.sessions}</h2>
        <Button
          variant="secondary"
          busy={pending === 'others'}
          disabled={others.length === 0}
          onClick={() => void act('others', profileApi.revokeOtherSessions)}
        >
          {vi.profile.revokeOthers}
        </Button>
      </div>
      {(failure ?? sessions.error) ? <ErrorBanner title={messageFor(failure ?? sessions.error)} /> : null}
      <ul className="pw-profile-sessions" aria-busy={sessions.isPending}>
        {list.map((item) => (
          <SessionRow
            key={item.sessionId}
            item={item}
            timeZone={timeZone}
            busy={pending === item.sessionId}
            onSignOut={() =>
              item.current ? void logOut() : void act(item.sessionId, () => profileApi.revokeSession(item.sessionId))
            }
          />
        ))}
      </ul>
    </section>
  );
}

function SessionRow({
  item,
  timeZone,
  busy,
  onSignOut,
}: {
  item: DeviceSession;
  timeZone: string;
  busy: boolean;
  onSignOut(): void;
}) {
  const device = deviceName(item.userAgent, vi.profile.unknownDevice);
  return (
    <li className="pw-profile-session">
      <div className="pw-profile-session-text">
        <div className="pw-profile-session-name">
          <strong>{device}</strong>
          {item.current ? <StatusPill tone="route">{vi.profile.thisDevice}</StatusPill> : null}
        </div>
        <span className="pw-profile-muted pw-profile-session-meta">
          {vi.profile.sessionLine(
            item.ipAddress ?? '—',
            formatDayTime(item.lastUsedAt, timeZone),
            formatDayTime(item.expiresAt, timeZone).split(' ')[0] ?? '',
          )}
        </span>
      </div>
      <Button
        variant="quiet"
        busy={busy}
        aria-label={item.current ? undefined : vi.profile.signOutDevice(device)}
        onClick={onSignOut}
      >
        {vi.profile.signOut}
      </Button>
    </li>
  );
}
