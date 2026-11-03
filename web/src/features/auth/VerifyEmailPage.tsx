import { useEffect, useState, type FormEvent } from 'react';
import { Link, useLocation, useSearchParams } from 'react-router';
import { ApiError } from '@/data/apiError';
import { errorMessages, fieldErrors, fieldMessage, formatWait, messageFor } from '@/shared/errors';
import { errorCode } from '@/shared/forms/serverErrors';
import { formatClock } from '@/shared/format';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { vi } from '@/shared/i18n/vi';
import { Button, buttonClass, ErrorBanner, MailIcon, OtpInput, TextField } from '@/shared/ui';
import { useAccountApi } from './accountApi';
import { AuthCardHead, AuthLayout, CompactArt } from './AuthLayout';
import { EMAIL_PATTERN, MAX_OTP_TRIES, RESEND_COOLDOWN_SECONDS } from './rules';

type Outcome = 'verified' | 'alreadyVerified' | null;

/**
 * Prototype "Xác thực email" (SCREENS §1.1): six boxes, resend locked for 60 s after a code was sent, three wrong
 * codes cancel the code. `?email=` comes from "Đăng ký" or from "Đăng nhập" (EMAIL_NOT_VERIFIED).
 */
export function VerifyEmailPage() {
  const accountApi = useAccountApi();
  const [params] = useSearchParams();
  const location = useLocation();
  const emailFromUrl = params.get('email')?.trim() ?? '';
  const [emailInput, setEmailInput] = useState('');
  const email = emailFromUrl || emailInput.trim();
  const [code, setCode] = useState('');
  const [wrongTries, setWrongTries] = useState(0);
  const [lastWrong, setLastWrong] = useState(false);
  const [emailError, setEmailError] = useState<string | null>(null);
  const [failure, setFailure] = useState<unknown>(null);
  const [resent, setResent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [resending, setResending] = useState(false);
  const [outcome, setOutcome] = useState<Outcome>(null);
  const cooldown = useCountdown();
  const codeSent = (location.state as { codeSent?: boolean } | null)?.codeSent === true;
  const { start: startCooldown } = cooldown;

  useEffect(() => {
    if (codeSent) startCooldown(RESEND_COOLDOWN_SECONDS);
  }, [codeSent, startCooldown]);

  const tooManyTries = wrongTries >= MAX_OTP_TRIES;

  const checkEmail = (): boolean => {
    if (emailFromUrl) return true;
    if (!email) setEmailError(fieldMessage('EMAIL_REQUIRED', vi.auth.email));
    else if (!EMAIL_PATTERN.test(email)) setEmailError(fieldMessage('INVALID_EMAIL_FORMAT', vi.auth.email));
    else {
      setEmailError(null);
      return true;
    }
    return false;
  };

  const onVerify = async (event: FormEvent) => {
    event.preventDefault();
    if (code.length !== 6 || tooManyTries || !checkEmail()) return;
    setBusy(true);
    setFailure(null);
    setResent(false);
    try {
      await accountApi.verifyEmail({ email, otp: code });
      setOutcome('verified');
    } catch (error) {
      const codeOfError = errorCode(error);
      if (codeOfError === 'INVALID_OTP') {
        setWrongTries((tries) => tries + 1);
        setLastWrong(true);
      } else if (codeOfError === 'EMAIL_ALREADY_VERIFIED') {
        setOutcome('alreadyVerified');
      } else if (
        codeOfError === 'VALIDATION_ERROR' &&
        !emailFromUrl &&
        fieldErrors(error, { email: vi.auth.email }).email
      ) {
        setEmailError(fieldErrors(error, { email: vi.auth.email }).email ?? null);
      } else {
        setFailure(error);
      }
    } finally {
      setBusy(false);
    }
  };

  const onResend = async () => {
    if (!checkEmail()) return;
    setResending(true);
    setFailure(null);
    try {
      await accountApi.resendVerification(email);
      setWrongTries(0);
      setLastWrong(false);
      setCode('');
      setResent(true);
      cooldown.start(RESEND_COOLDOWN_SECONDS);
    } catch (error) {
      if (error instanceof ApiError && error.code === 'RATE_LIMITED' && error.retryAfter !== null) {
        cooldown.start(error.retryAfter);
      }
      setFailure(error);
    } finally {
      setResending(false);
    }
  };

  const otpMessage = tooManyTries
    ? vi.form.otpTooManyTries
    : lastWrong
      ? (errorMessages.INVALID_OTP ?? '')
      : vi.form.otpRule;
  const lead = emailFromUrl ? (
    <>
      {vi.verify.lead(emailFromUrl)[0]}
      <strong className="pw-auth-strong">{emailFromUrl}</strong>
      {vi.verify.lead(emailFromUrl)[2]}
    </>
  ) : (
    vi.verify.leadNoEmail
  );

  return (
    <AuthLayout
      panel="compact"
      logoToLogin
      art={<CompactArt picture="mail" />}
      pitch={<p className="pw-auth-note">{vi.verify.brandLead}</p>}
    >
      <form className="pw-auth-card pw-auth-card-md" aria-labelledby="v-title" onSubmit={onVerify} noValidate>
        <AuthCardHead id="v-title" title={vi.verify.title} lead={lead} />

        {outcome ? (
          <>
            <div className="pw-auth-done" role="status">
              <strong>{outcome === 'verified' ? vi.verify.doneTitle : errorMessages.EMAIL_ALREADY_VERIFIED}</strong>
              <span>{vi.verify.doneLead}</span>
            </div>
            <Link className={buttonClass('primary', 'lg', true)} to="/login">
              {vi.form.toLogin}
            </Link>
          </>
        ) : (
          <>
            {failure ? <ErrorBanner title={messageFor(failure)} /> : null}
            {!emailFromUrl ? (
              <TextField
                label={vi.auth.email}
                type="email"
                required
                size="lg"
                autoComplete="email"
                inputMode="email"
                placeholder="ten@example.com"
                startIcon={<MailIcon />}
                value={emailInput}
                error={emailError ?? undefined}
                onChange={(event) => setEmailInput(event.target.value)}
              />
            ) : null}
            <div className="pw-auth-otp">
              <OtpInput
                label={vi.form.otp}
                value={code}
                onChange={(value) => {
                  setCode(value);
                  setLastWrong(false);
                }}
                invalid={lastWrong || tooManyTries}
                describedBy="otp-msg"
              />
              <p
                id="otp-msg"
                className={lastWrong || tooManyTries ? 'pw-field-msg pw-field-msg-error' : 'pw-field-msg'}
                aria-live="polite"
              >
                {otpMessage}
              </p>
            </div>
            <Button
              type="submit"
              variant="primary"
              size="lg"
              block
              busy={busy}
              disabled={code.length !== 6 || tooManyTries}
            >
              {vi.verify.submit}
            </Button>
            <div className="pw-auth-resend">
              <span>{resent ? vi.form.resent : vi.form.checkSpam}</span>
              <Button
                variant="quiet"
                busy={resending}
                disabled={cooldown.remaining > 0}
                onClick={() => void onResend()}
              >
                {cooldown.remaining > 0
                  ? vi.form.resendIn(
                      cooldown.remaining > 120 ? formatWait(cooldown.remaining) : formatClock(cooldown.remaining),
                    )
                  : vi.form.resend}
              </Button>
            </div>
          </>
        )}
      </form>
    </AuthLayout>
  );
}
