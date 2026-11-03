import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { ApiError } from '@/data/apiError';
import { errorMessages, fieldErrors, formatWait, messageFor } from '@/shared/errors';
import { errorCode } from '@/shared/forms/serverErrors';
import { formatClock } from '@/shared/format';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { vi } from '@/shared/i18n/vi';
import { Button, buttonClass, ErrorBanner, TextField } from '@/shared/ui';
import { useAccountApi } from './accountApi';
import { AuthCardHead, AuthLayout, CompactArt } from './AuthLayout';
import { emailRules, MAX_OTP_TRIES, passwordRules, RESEND_COOLDOWN_SECONDS } from './rules';

type Step = 0 | 1 | 2 | 3;

const check = (rule: { validate: (value: string) => true | string }, value: string) => {
  const result = rule.validate(value);
  return result === true ? null : result;
};

/**
 * Prototype "Quên mật khẩu" (SCREENS §1.1): email → code → new password. Step 1 always says the code was sent if the
 * email has an account; step 3 must be done within the 10 minutes of the reset token; afterwards every device is
 * signed out.
 */
export function ForgotPasswordPage() {
  const accountApi = useAccountApi();
  const [step, setStep] = useState<Step>(0);
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [errors, setErrors] = useState<Record<string, string | null>>({});
  const [failure, setFailure] = useState<unknown>(null);
  const [tokenExpired, setTokenExpired] = useState(false);
  const [wrongTries, setWrongTries] = useState(0);
  const [busy, setBusy] = useState(false);
  const lock = useCountdown();
  const cooldown = useCountdown();

  const fail = (error: unknown) => {
    if (error instanceof ApiError && error.code === 'RATE_LIMITED' && error.retryAfter !== null) {
      lock.start(error.retryAfter);
    }
    setFailure(error);
  };

  const run = async (action: () => Promise<void>) => {
    setBusy(true);
    setFailure(null);
    try {
      await action();
    } finally {
      setBusy(false);
    }
  };

  const sendCode = async (event?: FormEvent) => {
    event?.preventDefault();
    const emailError = check(emailRules(vi.auth.email), email);
    setErrors({ email: emailError });
    if (emailError) return;
    await run(async () => {
      try {
        await accountApi.forgotPassword(email.trim());
        setWrongTries(0);
        setCode('');
        cooldown.start(RESEND_COOLDOWN_SECONDS);
        setStep(1);
      } catch (error) {
        const field = fieldErrors(error, { email: vi.auth.email }).email;
        if (errorCode(error) === 'VALIDATION_ERROR' && field) setErrors({ email: field });
        else fail(error);
      }
    });
  };

  const verifyCode = async (event: FormEvent) => {
    event.preventDefault();
    if (code.length !== 6 || wrongTries >= MAX_OTP_TRIES) return;
    await run(async () => {
      try {
        const result = await accountApi.verifyResetOtp({ email: email.trim(), otp: code });
        setResetToken(result.resetToken);
        setErrors({});
        setStep(2);
      } catch (error) {
        if (errorCode(error) === 'INVALID_OTP') {
          const tries = wrongTries + 1;
          setWrongTries(tries);
          setErrors({
            code: tries >= MAX_OTP_TRIES ? vi.form.otpTooManyTries : (errorMessages.INVALID_OTP ?? ''),
          });
        } else fail(error);
      }
    });
  };

  const resetPassword = async (event: FormEvent) => {
    event.preventDefault();
    const passwordError = check(passwordRules(vi.form.newPassword), password);
    const confirmError = confirm !== password ? vi.form.passwordMismatch : null;
    setErrors({ password: passwordError, confirm: confirmError });
    if (passwordError || confirmError) return;
    await run(async () => {
      try {
        await accountApi.resetPassword({ resetToken, newPassword: password });
        setStep(3);
      } catch (error) {
        const field = fieldErrors(error, { newPassword: vi.form.newPassword }).newPassword;
        if (errorCode(error) === 'INVALID_RESET_TOKEN') setTokenExpired(true);
        else if (errorCode(error) === 'VALIDATION_ERROR' && field) setErrors({ password: field });
        else fail(error);
      }
    });
  };

  const restart = () => {
    setStep(0);
    setCode('');
    setPassword('');
    setConfirm('');
    setResetToken('');
    setTokenExpired(false);
    setErrors({});
    setFailure(null);
  };

  const submit = [sendCode, verifyCode, resetPassword, undefined][step];

  return (
    <AuthLayout
      panel="compact"
      logoToLogin
      art={<CompactArt picture="lock" />}
      pitch={<p className="pw-auth-note">{vi.forgot.brandLead}</p>}
    >
      <form
        className="pw-auth-card pw-auth-card-md"
        aria-labelledby="f-title"
        onSubmit={submit ? (event) => void submit(event) : (event) => event.preventDefault()}
        noValidate
      >
        <AuthCardHead id="f-title" title={vi.forgot.titles[step]} lead={vi.forgot.leads[step]} accent={false} />
        <ol className="pw-auth-steps" aria-label={vi.forgot.stepsLabel}>
          {vi.forgot.steps.map((label, index) => (
            <li
              key={label}
              className={step >= index ? 'is-on' : undefined}
              aria-current={step === index ? 'step' : undefined}
            >
              <span />
              {label}
            </li>
          ))}
        </ol>

        {failure ? <ErrorBanner title={messageFor(failure)} /> : null}

        {step === 0 ? (
          <>
            <TextField
              label={vi.auth.email}
              type="email"
              required
              autoComplete="email"
              inputMode="email"
              placeholder="ten@example.com"
              value={email}
              error={errors.email ?? undefined}
              onChange={(event) => setEmail(event.target.value)}
            />
            <Button type="submit" variant="primary" size="lg" block busy={busy} disabled={lock.remaining > 0}>
              {lock.remaining > 0 ? vi.auth.retryIn(formatWait(lock.remaining)) : vi.forgot.sendCode}
            </Button>
          </>
        ) : null}

        {step === 1 ? (
          <>
            <div className="pw-auth-notice" role="status">
              {vi.forgot.sent}
            </div>
            <TextField
              className="pw-field-code"
              label={vi.form.otp}
              inputMode="numeric"
              autoComplete="one-time-code"
              maxLength={6}
              placeholder="••••••"
              value={code}
              hint={errors.code ? undefined : vi.form.otpRule}
              error={errors.code ?? undefined}
              onChange={(event) => {
                setCode(event.target.value.replace(/\D/g, '').slice(0, 6));
                if (wrongTries < MAX_OTP_TRIES) setErrors({});
              }}
            />
            <Button
              type="submit"
              variant="primary"
              size="lg"
              block
              busy={busy}
              disabled={code.length !== 6 || wrongTries >= MAX_OTP_TRIES}
            >
              {vi.forgot.next}
            </Button>
            <Button
              variant="quiet"
              disabled={cooldown.remaining > 0 || lock.remaining > 0 || busy}
              onClick={() => void sendCode()}
            >
              {cooldown.remaining > 0 ? vi.form.resendIn(formatClock(cooldown.remaining)) : vi.form.resend}
            </Button>
            <Button variant="quiet" onClick={restart}>
              {vi.forgot.changeEmail}
            </Button>
          </>
        ) : null}

        {step === 2 ? (
          tokenExpired ? (
            <>
              <ErrorBanner title={errorMessages.INVALID_RESET_TOKEN} />
              <Button variant="primary" size="lg" block onClick={restart}>
                {vi.forgot.restart}
              </Button>
            </>
          ) : (
            <>
              <TextField
                label={vi.form.newPassword}
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
                label={vi.form.confirmPassword}
                type="password"
                required
                maxLength={72}
                autoComplete="new-password"
                value={confirm}
                error={errors.confirm ?? undefined}
                onChange={(event) => setConfirm(event.target.value)}
              />
              <p className="pw-auth-muted">{vi.forgot.deadline}</p>
              <Button type="submit" variant="primary" size="lg" block busy={busy}>
                {vi.forgot.submit}
              </Button>
            </>
          )
        ) : null}

        {step === 3 ? (
          <>
            <div className="pw-auth-done" role="status">
              <strong>{vi.forgot.doneTitle}</strong>
              <span>{vi.forgot.doneLead}</span>
            </div>
            <Link className={buttonClass('primary', 'lg', true)} to="/login">
              {vi.form.toLogin}
            </Link>
          </>
        ) : null}

        {step !== 3 ? (
          <p className="pw-auth-center">
            {vi.form.rememberPassword} <Link to="/login">{vi.form.toLogin}</Link>
          </p>
        ) : null}
      </form>
    </AuthLayout>
  );
}
