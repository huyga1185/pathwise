import { useState, type FormEvent } from 'react';
import { Link } from 'react-router';
import { ApiError } from '@/data/apiError';
import { fieldErrors, formatWait, messageFor } from '@/shared/errors';
import { vi } from '@/shared/i18n/vi';
import {
  ArrowRightIcon,
  Button,
  CheckCircleIcon,
  ErrorBanner,
  EyeIcon,
  EyeOffIcon,
  LockIcon,
  MailIcon,
  PhoneIcon,
  RouteIcon,
  TextField,
} from '@/shared/ui';
import { useCountdown } from '@/shared/hooks/useCountdown';
import { AuthCardHead, AuthLayout, MainArt, RouteArt } from './AuthLayout';
import { useSession } from './session';

const FIELD_LABELS = { email: vi.auth.email, password: vi.auth.password };
const FEATURE_ICONS = [RouteIcon, PhoneIcon, CheckCircleIcon];

/** Prototype "Đăng nhập" (docs/client/SCREENS.md §1.1). */
export function LoginPage() {
  const { session, logIn } = useSession();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<ApiError | Error | null>(null);
  const lock = useCountdown();

  const expired = session.status === 'anonymous' && session.expired;

  const onSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      // RedirectIfSignedIn sends the user to ?next= or their home once the session is set.
      await logIn(email.trim(), password);
    } catch (caught) {
      const failure =
        caught instanceof ApiError ? caught : caught instanceof Error ? caught : new Error(String(caught));
      setError(failure);
      if (failure instanceof ApiError && failure.code === 'RATE_LIMITED' && failure.retryAfter !== null) {
        lock.start(failure.retryAfter);
      }
    } finally {
      setBusy(false);
    }
  };

  const fields = error instanceof ApiError && error.code === 'VALIDATION_ERROR' ? fieldErrors(error, FIELD_LABELS) : {};
  const remaining = lock.remaining;
  const banner = error && !(error instanceof ApiError && error.code === 'VALIDATION_ERROR') ? messageFor(error) : null;
  const [message, detail] = banner ? splitSentence(banner) : [null, null];

  return (
    <AuthLayout
      panel="wide"
      art={<RouteArt />}
      mainArt={<MainArt />}
      pitch={
        <div className="pw-auth-pitch">
          <p className="pw-auth-headline">
            {vi.auth.brandHeadline[0]}
            <span>{vi.auth.brandHeadline[1]}</span>
            {vi.auth.brandHeadline[2]}
          </p>
          <p className="pw-auth-lead">{vi.auth.brandLead}</p>
          <ul className="pw-auth-features">
            {vi.auth.features.map(([first, second], index) => {
              const FeatureIcon = FEATURE_ICONS[index] ?? CheckCircleIcon;
              return (
                <li key={first}>
                  <FeatureIcon />
                  <span>
                    {first}
                    <br />
                    {second}
                  </span>
                </li>
              );
            })}
          </ul>
        </div>
      }
    >
      <form className="pw-auth-card" aria-labelledby="login-title" onSubmit={onSubmit} noValidate>
        <AuthCardHead id="login-title" title={vi.auth.loginTitle} lead={vi.auth.loginLead} />

        {expired && !error ? <ErrorBanner title={vi.auth.sessionExpired} tone="info" /> : null}
        {message ? (
          <ErrorBanner title={message}>
            {detail}
            {error instanceof ApiError && error.code === 'EMAIL_NOT_VERIFIED' ? (
              <>
                {' '}
                <Link to={`/verify-email?email=${encodeURIComponent(email.trim())}`}>{vi.auth.verifyLink}</Link>
              </>
            ) : null}
          </ErrorBanner>
        ) : null}

        <TextField
          label={vi.auth.email}
          type="email"
          autoComplete="email"
          inputMode="email"
          required
          size="lg"
          placeholder="ten@example.com"
          startIcon={<MailIcon />}
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={fields.email}
        />
        <TextField
          label={vi.auth.password}
          type={showPassword ? 'text' : 'password'}
          autoComplete="current-password"
          required
          size="lg"
          startIcon={<LockIcon />}
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={fields.password}
          endAction={
            <button
              type="button"
              className="pw-icon-button"
              aria-label={showPassword ? vi.auth.hidePassword : vi.auth.showPassword}
              aria-pressed={showPassword}
              onClick={() => setShowPassword((shown) => !shown)}
            >
              {showPassword ? <EyeOffIcon /> : <EyeIcon />}
            </button>
          }
        />
        <div className="pw-auth-forgot">
          <Link to="/forgot-password">{vi.auth.forgot}</Link>
        </div>

        <Button type="submit" variant="primary" size="lg" block busy={busy} disabled={remaining > 0}>
          {remaining > 0 ? (
            vi.auth.retryIn(formatWait(remaining))
          ) : (
            <>
              {vi.auth.submit}
              <ArrowRightIcon />
            </>
          )}
        </Button>

        <div className="pw-auth-or" aria-hidden="true">
          <span />
          {vi.auth.or}
          <span />
        </div>
        <div className="pw-auth-links">
          <span>
            {vi.auth.noAccount} <Link to="/register">{vi.auth.register}</Link>
          </span>
          <span>
            {vi.auth.invited} <Link to="/invite">{vi.auth.enterInvite}</Link>
          </span>
        </div>
      </form>
    </AuthLayout>
  );
}

/** "Tài khoản đã bị vô hiệu hoá. Liên hệ chủ cửa hàng." → title and detail. */
function splitSentence(text: string): [string, string | null] {
  const cut = text.indexOf('. ');
  return cut > 0 ? [text.slice(0, cut + 1), text.slice(cut + 2)] : [text, null];
}
