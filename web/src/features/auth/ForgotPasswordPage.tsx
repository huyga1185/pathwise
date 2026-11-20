import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { ApiError } from '@/data/apiError';
import { errorMessages, fieldErrors, formatWait, messageFor } from '@/shared/errors';
import { errorCode } from '@/shared/forms/serverErrors';
import { formatClock } from '@/shared/format';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { vi } from '@/shared/i18n/vi';
import {
  Button,
  buttonClass,
  CheckCircleIcon,
  CodeField,
  ErrorBanner,
  KeyIcon,
  LockIcon,
  MailIcon,
  TextField,
} from '@/shared/ui';
import { useAccountApi } from './accountApi';
import { AuthCardHead, AuthLayout, MainArt, MapArt } from './AuthLayout';
import { changeCard } from './changeCard';
import { emailRules, MAX_OTP_TRIES, passwordRules, RESEND_COOLDOWN_SECONDS } from './rules';

type Step = 0 | 1 | 2 | 3;

const STEP_ICONS = [MailIcon, KeyIcon, LockIcon];

const check = (rule: { validate: (value: string) => true | string }, value: string) => {
  const result = rule.validate(value);
  return result === true ? null : result;
};

/**
 * Prototype "Quên mật khẩu" (SCREENS §1.1): email → code → new password. Step 1 always says the code was sent if the
 * email has an account; step 3 must be done within the 10 minutes of the reset token; afterwards every device is
 * signed out. Same picture, card and controls as "Đăng nhập" (AuthStage, SCREENS.md §1.2).
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

  // The card changes its content in place: move focus to the new title so screen readers report it (HIG VoiceOver:
  // "Inform VoiceOver when visible content or layout changes occur") and the keyboard is not left on a gone button.
  const heading = useRef<HTMLHeadingElement>(null);
  const shownStep = useRef(step);
  useEffect(() => {
    if (shownStep.current === step) return;
    shownStep.current = step;
    heading.current?.focus();
  }, [step]);

  // HIG Text fields: an email is checked "when people switch to another field", a new password "before people switch
  // to another field" (while typed). The repeat is checked when left: while it is typed it differs anyway.
  const checkEmail = () => setErrors((current) => ({ ...current, email: check(emailRules(vi.auth.email), email) }));
  const checkPassword = (value: string) =>
    setErrors((current) => ({ ...current, password: check(passwordRules(vi.form.newPassword), value) }));
  const checkConfirm = () =>
    setErrors((current) => ({
      ...current,
      confirm: confirm !== '' && confirm !== password ? vi.form.passwordMismatch : null,
    }));

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
        cooldown.start(RESEND_COOLDOWN_SECONDS);
        // From step 1 only: "Gửi lại mã" on step 2 stays where it is.
        const next = () => {
          setWrongTries(0);
          setCode('');
          setStep(1);
        };
        if (step === 0) changeCard(next);
        else next();
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
        changeCard(() => {
          setResetToken(result.resetToken);
          setErrors({});
          setStep(2);
        });
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
        changeCard(() => setStep(3));
      } catch (error) {
        const field = fieldErrors(error, { newPassword: vi.form.newPassword }).newPassword;
        if (errorCode(error) === 'INVALID_RESET_TOKEN') changeCard(() => setTokenExpired(true));
        else if (errorCode(error) === 'VALIDATION_ERROR' && field) setErrors({ password: field });
        else fail(error);
      }
    });
  };

  const restart = () =>
    changeCard(() => {
      setStep(0);
      setCode('');
      setPassword('');
      setConfirm('');
      setResetToken('');
      setTokenExpired(false);
      setErrors({});
      setFailure(null);
    });

  const submit = [sendCode, verifyCode, resetPassword, undefined][step];

  return (
    <AuthLayout
      panel="wide"
      logoToLogin
      art={<MapArt />}
      mainArt={<MainArt variant="register" />}
      pitch={
        <div className="pw-auth-pitch pw-auth-pitch-steps">
          <div className="pw-auth-pitch-text">
            <p className="pw-auth-headline">
              {vi.forgot.brandHeadline[0]}
              <span>{vi.forgot.brandHeadline[1]}</span>
              {vi.forgot.brandHeadline[2]}
            </p>
            <p className="pw-auth-lead">{vi.forgot.brandLead}</p>
          </div>
          <ol className="pw-auth-steps-inline">
            {vi.forgot.steps.map((item, index) => {
              const StepIcon = STEP_ICONS[index] ?? CheckCircleIcon;
              return (
                <li key={item}>
                  <StepIcon />
                  <span>{item}</span>
                </li>
              );
            })}
          </ol>
        </div>
      }
    >
      <form
        className="pw-auth-card"
        aria-labelledby="f-title"
        onSubmit={submit ? (event) => void submit(event) : (event) => event.preventDefault()}
        noValidate
      >
        <AuthCardHead id="f-title" title={vi.forgot.titles[step]} accent={false} headingRef={heading} />
        {step < 3 ? (
          // Determinate progress bar of the three steps (HIG Progress indicators: a track that "fills from the leading
          // side", with a short description). It fills with the steps already done, "as accurate as possible": empty
          // on step 1, two thirds on step 3. Transient like Apple's: gone once the task is done (step "Xong").
          <div className="pw-auth-progress">
            <div
              className="pw-auth-progress-track"
              role="progressbar"
              aria-label={vi.forgot.progressLabel}
              aria-valuemin={0}
              aria-valuemax={vi.forgot.steps.length}
              aria-valuenow={step}
              aria-valuetext={`${vi.forgot.progress(step + 1, vi.forgot.steps.length)}: ${vi.forgot.steps[step as 0 | 1 | 2]}`}
            >
              <span style={{ width: `${(step / vi.forgot.steps.length) * 100}%` }}>
                {/* One glint over the done part when a step is done (a new element per step, so it runs once). */}
                <i key={step} className="pw-auth-progress-shine" />
              </span>
            </div>
            <span className="pw-auth-progress-text" aria-hidden="true">
              {vi.forgot.progress(step + 1, vi.forgot.steps.length)}
            </span>
          </div>
        ) : null}

        {failure ? <ErrorBanner title={messageFor(failure)} /> : null}

        {step === 0 ? (
          <>
            <TextField
              label={vi.auth.email}
              clearable
              type="email"
              required
              size="lg"
              startIcon={<MailIcon />}
              autoComplete="email"
              inputMode="email"
              placeholder="ten@example.com"
              value={email}
              error={errors.email ?? undefined}
              onChange={(event) => setEmail(event.target.value)}
              onBlur={checkEmail}
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
            <CodeField
              label={vi.form.otp}
              value={code}
              hint={errors.code ? undefined : vi.form.otpRule}
              error={errors.code ?? undefined}
              onChange={(value) => {
                setCode(value);
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
            <div className="pw-auth-actions">
              <Button
                variant="quiet"
                size="lg"
                disabled={cooldown.remaining > 0 || lock.remaining > 0 || busy}
                onClick={() => void sendCode()}
              >
                {cooldown.remaining > 0 ? vi.form.resendIn(formatClock(cooldown.remaining)) : vi.form.resend}
              </Button>
              <Button variant="quiet" size="lg" onClick={restart}>
                {vi.forgot.changeEmail}
              </Button>
            </div>
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
                size="lg"
                startIcon={<LockIcon />}
                maxLength={72}
                autoComplete="new-password"
                hint={vi.form.passwordHint}
                value={password}
                error={errors.password ?? undefined}
                onChange={(event) => {
                  setPassword(event.target.value);
                  checkPassword(event.target.value);
                }}
              />
              <TextField
                label={vi.form.confirmPassword}
                type="password"
                required
                size="lg"
                startIcon={<LockIcon />}
                maxLength={72}
                autoComplete="new-password"
                value={confirm}
                error={errors.confirm ?? undefined}
                onChange={(event) => setConfirm(event.target.value)}
                onBlur={checkConfirm}
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
            <Link className={buttonClass('primary', 'lg', true)} to="/login" viewTransition>
              {vi.form.toLogin}
            </Link>
          </>
        ) : null}

        {step !== 3 ? (
          <p className="pw-auth-center">
            {vi.form.rememberPassword}{' '}
            <Link to="/login" viewTransition>
              {vi.form.toLogin}
            </Link>
          </p>
        ) : null}
      </form>
    </AuthLayout>
  );
}
