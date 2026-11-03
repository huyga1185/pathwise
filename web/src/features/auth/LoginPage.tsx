import { useEffect, useState, type FormEvent } from 'react';
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
  LogoMark,
  MailIcon,
  PhoneIcon,
  RouteIcon,
  TextField,
} from '@/shared/ui';
import { useSession } from './session';
import './login.css';

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
  const [lockedUntil, setLockedUntil] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());

  const expired = session.status === 'anonymous' && session.expired;

  useEffect(() => {
    if (lockedUntil === null) return undefined;
    const timer = window.setInterval(() => {
      setNow(Date.now());
      if (Date.now() >= lockedUntil) setLockedUntil(null);
    }, 1000);
    return () => window.clearInterval(timer);
  }, [lockedUntil]);

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
        setNow(Date.now());
        setLockedUntil(Date.now() + failure.retryAfter * 1000);
      }
    } finally {
      setBusy(false);
    }
  };

  const fields = error instanceof ApiError && error.code === 'VALIDATION_ERROR' ? fieldErrors(error, FIELD_LABELS) : {};
  const remaining = lockedUntil !== null ? Math.max(0, Math.ceil((lockedUntil - now) / 1000)) : 0;
  const banner = error && !(error instanceof ApiError && error.code === 'VALIDATION_ERROR') ? messageFor(error) : null;
  const [message, detail] = banner ? splitSentence(banner) : [null, null];

  return (
    <div className="pw-auth">
      <section className="pw-auth-brand" aria-label="Giới thiệu PathWise">
        <BrandArt />
        <div className="pw-auth-logo">
          <LogoMark white size={40} />
          <span>{vi.appName}</span>
        </div>
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
      </section>

      <main className="pw-auth-main">
        <MainArt />
        <form className="pw-auth-card" aria-labelledby="login-title" onSubmit={onSubmit} noValidate>
          <div className="pw-auth-card-head">
            <h1 id="login-title">{vi.auth.loginTitle}</h1>
            <p>{vi.auth.loginLead}</p>
            <span className="pw-auth-accent" aria-hidden="true" />
          </div>

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
      </main>
    </div>
  );
}

/** "Tài khoản đã bị vô hiệu hoá. Liên hệ chủ cửa hàng." → title and detail. */
function splitSentence(text: string): [string, string | null] {
  const cut = text.indexOf('. ');
  return cut > 0 ? [text.slice(0, cut + 1), text.slice(cut + 2)] : [text, null];
}

/**
 * Artwork of the brand panel, drawn exactly as the prototype "Đăng nhập": one 720×720 drawing (street grid, blobs,
 * skyline, route and pin) that covers the panel, so the route keeps the prototype's place.
 */
function BrandArt() {
  return (
    <svg
      className="pw-auth-art"
      viewBox="0 0 720 720"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="40" cy="20" r="210" fill="#ffffff" fillOpacity="0.06" />
      <circle cx="700" cy="760" r="260" fill="#ffffff" fillOpacity="0.05" />
      <g transform="rotate(-24 360 360)" stroke="#ffffff" fill="none">
        <g strokeOpacity="0.09" strokeWidth="12">
          <path d="M-200 120 H920M-200 300 H920M-200 480 H920M-200 660 H920M60 -200 V920M260 -200 V920M460 -200 V920M660 -200 V920" />
        </g>
        <g strokeOpacity="0.05" strokeWidth="3">
          <path d="M-200 210 H920M-200 390 H920M-200 570 H920M160 -200 V920M360 -200 V920M560 -200 V920" />
        </g>
      </g>
      <g fill="#ffffff" fillOpacity="0.07">
        <rect x="520" y="560" width="26" height="160" />
        <rect x="550" y="520" width="34" height="200" />
        <rect x="588" y="590" width="22" height="130" />
        <rect x="614" y="540" width="30" height="180" />
        <rect x="648" y="600" width="24" height="120" />
        <rect x="676" y="570" width="44" height="150" />
      </g>
      <g fill="#ffffff" fillOpacity="0.18">
        <path d="M386 268 s-12 -11 -12 -20 a12 12 0 0 1 24 0 c0 9 -12 20 -12 20z" />
        <path d="M676 306 s-12 -11 -12 -20 a12 12 0 0 1 24 0 c0 9 -12 20 -12 20z" />
      </g>
      <path
        d="M150 500 C 190 440, 230 418, 278 418 S 380 360, 442 324 S 540 290, 543 236"
        fill="none"
        stroke="#ffffff"
        strokeWidth="5"
        strokeDasharray="12 11"
        strokeLinecap="round"
      />
      <circle cx="278" cy="418" r="17" fill="#ffffff" />
      <text
        x="278"
        y="424"
        textAnchor="middle"
        fontFamily="Be Vietnam Pro, sans-serif"
        fontSize="16"
        fontWeight="700"
        fill="#0b6a5c"
      >
        1
      </text>
      <circle cx="442" cy="324" r="17" fill="#ffffff" />
      <text
        x="442"
        y="330"
        textAnchor="middle"
        fontFamily="Be Vietnam Pro, sans-serif"
        fontSize="16"
        fontWeight="700"
        fill="#0b6a5c"
      >
        2
      </text>
      <ellipse cx="543" cy="228" rx="34" ry="11" fill="#ffffff" fillOpacity="0.18" />
      <ellipse cx="543" cy="228" rx="18" ry="6" fill="#ffffff" fillOpacity="0.35" />
      <path d="M543 224 s-36 -32 -36 -60 a36 36 0 0 1 72 0 c0 28 -36 60 -36 60z" fill="#ffffff" />
      <circle cx="543" cy="164" r="13" fill="#0b6a5c" />
    </svg>
  );
}

/** Soft shapes behind the card (prototype): tint circles in two corners and a faint pin. */
function MainArt() {
  return (
    <svg
      className="pw-auth-art"
      viewBox="0 0 600 720"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="600" cy="0" r="170" className="pw-auth-tint" fillOpacity="0.7" />
      <circle cx="600" cy="0" r="110" className="pw-auth-tint" />
      <circle cx="560" cy="760" r="200" className="pw-auth-tint" fillOpacity="0.6" />
      <path
        d="M520 640 s-22 -20 -22 -37 a22 22 0 0 1 44 0 c0 17 -22 37 -22 37z"
        className="pw-auth-pin"
        fillOpacity="0.18"
      />
    </svg>
  );
}
