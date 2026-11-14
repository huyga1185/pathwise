import { useState, type FormEvent } from 'react';
import { Link, useSearchParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { errorMessages, fieldErrors, formatWait, messageFor } from '@/shared/errors';
import { errorCode } from '@/shared/forms/serverErrors';
import { formatClock } from '@/shared/format';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { vi } from '@/shared/i18n/vi';
import { Button, buttonClass, ErrorBanner, TextField } from '@/shared/ui';
import { useAccountApi } from './accountApi';
import { AuthCardHead, AuthLayout, CompactArt } from './AuthLayout';
import { MAX_OTP_TRIES, passwordRules, RESEND_COOLDOWN_SECONDS } from './rules';

type Step = 'start' | 'code' | 'done' | 'expired';

/**
 * Prototype "Đặt lại mật khẩu từ link" (W11, T57, P-63): the page of the link an ADMIN emailed. It never shows or asks
 * the email: "Gửi mã" sends a code to the account's own email, then code and new password set it. A wrong, expired or
 * used link replaces the page with a way to "Quên mật khẩu".
 */
export function ResetPasswordPage() {
  const token = (useSearchParams()[0].get('token') ?? '').trim();
  const accountApi = useAccountApi();
  const t = vi.resetLink;
  const [step, setStep] = useState<Step>(token ? 'start' : 'expired');
  const [code, setCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState<Record<string, string | null>>({});
  const [wrongTries, setWrongTries] = useState(0);
  const [failure, setFailure] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const cooldown = useCountdown();
  const lock = useCountdown();

  const fail = (error: unknown) => {
    const codeOf = errorCode(error);
    if (codeOf === 'INVALID_RESET_TOKEN' || codeOf === 'VALIDATION_ERROR') return setStep('expired');
    if (
      error instanceof ApiError &&
      (codeOf === 'RATE_LIMITED' || codeOf === 'OTP_COOLDOWN') &&
      error.retryAfter !== null
    ) {
      lock.start(error.retryAfter);
    }
    setFailure(error);
  };

  const sendCode = async () => {
    setBusy(true);
    setFailure(null);
    try {
      await accountApi.sendResetLinkOtp(token);
      setWrongTries(0);
      setErrors({});
      cooldown.start(RESEND_COOLDOWN_SECONDS);
      setStep('code');
    } catch (error) {
      fail(error);
    } finally {
      setBusy(false);
    }
  };

  const save = async (event: FormEvent) => {
    event.preventDefault();
    const rule = passwordRules(t.newPassword).validate(password);
    const next = {
      code: code.length === 6 ? null : t.codeRequired,
      password: rule === true ? null : rule,
      confirm: confirm === password ? null : t.mismatch,
    };
    setErrors(next);
    if (next.code || next.password || next.confirm || wrongTries >= MAX_OTP_TRIES) return;
    setBusy(true);
    setFailure(null);
    try {
      await accountApi.resetPasswordByLink({ token, otp: code, newPassword: password });
      setStep('done');
    } catch (error) {
      if (errorCode(error) === 'INVALID_OTP') {
        const tries = wrongTries + 1;
        setWrongTries(tries);
        setErrors({ code: tries >= MAX_OTP_TRIES ? vi.form.otpTooManyTries : (errorMessages.INVALID_OTP ?? '') });
      } else {
        const field = fieldErrors(error, { newPassword: t.newPassword }).newPassword;
        if (errorCode(error) === 'VALIDATION_ERROR' && field) setErrors({ password: field });
        else fail(error);
      }
    } finally {
      setBusy(false);
    }
  };

  const titles: Record<Step, string> = {
    start: t.startTitle,
    code: t.codeTitle,
    done: t.doneTitle,
    expired: t.expiredTitle,
  };
  const leads: Record<Step, string> = { start: t.startLead, code: t.codeLead, done: '', expired: '' };
  const resendLabel =
    lock.remaining > 0
      ? vi.auth.retryIn(formatWait(lock.remaining))
      : cooldown.remaining > 0
        ? t.resendIn(formatClock(cooldown.remaining))
        : t.resend;

  return (
    <AuthLayout
      panel="compact"
      logoToLogin
      art={<CompactArt picture="lock" />}
      pitch={<p className="pw-auth-note">{t.side}</p>}
    >
      <form
        className="pw-auth-card pw-auth-card-md"
        aria-labelledby="r-title"
        onSubmit={(event) => void save(event)}
        noValidate
      >
        <AuthCardHead id="r-title" title={titles[step]} lead={leads[step]} accent={false} />
        {failure ? <ErrorBanner title={messageFor(failure)} /> : null}

        {step === 'start' ? (
          <>
            <p>{t.startText}</p>
            <Button
              variant="primary"
              size="lg"
              block
              busy={busy}
              disabled={lock.remaining > 0}
              onClick={() => void sendCode()}
            >
              {lock.remaining > 0 ? vi.auth.retryIn(formatWait(lock.remaining)) : t.send}
            </Button>
          </>
        ) : null}

        {step === 'code' ? (
          <>
            <div className="pw-auth-notice" role="status">
              {t.sent}
            </div>
            <TextField
              className="pw-field-code"
              label={t.otp}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="••••••"
              value={code}
              hint={errors.code ? undefined : t.otpHint}
              error={errors.code ?? undefined}
              onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 6))}
            />
            <TextField
              label={t.newPassword}
              type="password"
              required
              maxLength={72}
              autoComplete="new-password"
              hint={vi.form.passwordHint}
              value={password}
              error={errors.password ?? undefined}
              onChange={(event) => setPassword(event.target.value)}
            />
            <TextField
              label={t.confirm}
              type="password"
              required
              maxLength={72}
              autoComplete="new-password"
              value={confirm}
              error={errors.confirm ?? undefined}
              onChange={(event) => setConfirm(event.target.value)}
            />
            <Button type="submit" variant="primary" size="lg" block busy={busy} disabled={wrongTries >= MAX_OTP_TRIES}>
              {t.save}
            </Button>
            <Button
              variant="quiet"
              disabled={cooldown.remaining > 0 || lock.remaining > 0 || busy}
              onClick={() => void sendCode()}
            >
              {resendLabel}
            </Button>
          </>
        ) : null}

        {step === 'done' ? (
          <>
            <div className="pw-auth-done" role="status">
              <strong>{t.done}</strong>
              <span>{t.doneText}</span>
            </div>
            <Link className={buttonClass('primary', 'lg', true)} to="/login">
              {t.login}
            </Link>
          </>
        ) : null}

        {step === 'expired' ? (
          <>
            <ErrorBanner title={t.expired}>{t.expiredText}</ErrorBanner>
            <Link className={buttonClass('secondary', 'lg', true)} to="/forgot-password">
              {t.forgot}
            </Link>
          </>
        ) : null}
      </form>
    </AuthLayout>
  );
}
