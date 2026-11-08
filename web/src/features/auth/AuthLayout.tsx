import type { ReactNode } from 'react';
import { Link } from 'react-router';
import { vi } from '@/shared/i18n/vi';
import { LogoMark } from '@/shared/ui';
import './login.css';

export interface AuthLayoutProps {
  /** wide: the large brand panel of "Đăng nhập" and "Đăng ký"; compact: the smaller one of the other account screens. */
  panel: 'wide' | 'compact';
  /** Decorative drawing covering the brand panel ({@link RouteArt} or {@link CompactArt}). */
  art: ReactNode;
  /** Text at the bottom of the brand panel. */
  pitch: ReactNode;
  /** The logo goes back to /login on every screen but /login itself. */
  logoToLogin?: boolean;
  /** Decorative shapes behind the card ({@link MainArt}). */
  mainArt?: ReactNode;
  /** The card (a form). */
  children: ReactNode;
}

/** Layout of the account screens (prototype "Đăng nhập", "Đăng ký cửa hàng", "Xác thực email"…): brand panel and card. */
export function AuthLayout({ panel, art, pitch, logoToLogin, mainArt, children }: AuthLayoutProps) {
  const compact = panel === 'compact';
  const logo = (
    <>
      <LogoMark white size={40} />
      <span>{vi.appName}</span>
    </>
  );
  return (
    <div className="pw-auth">
      <section
        className={compact ? 'pw-auth-brand pw-auth-brand-compact' : 'pw-auth-brand'}
        aria-label={compact ? vi.appName : 'Giới thiệu PathWise'}
      >
        {art}
        {logoToLogin ? (
          <Link to="/login" className="pw-auth-logo" aria-label={vi.form.toLoginLabel}>
            {logo}
          </Link>
        ) : (
          <div className="pw-auth-logo">{logo}</div>
        )}
        {pitch}
      </section>
      <main className={compact ? 'pw-auth-main pw-auth-main-compact' : 'pw-auth-main'}>
        {mainArt}
        {children}
      </main>
    </div>
  );
}

/** Title, lead and the accent bar at the top of an account card. */
export function AuthCardHead({
  id,
  title,
  lead,
  accent = true,
}: {
  id: string;
  title: ReactNode;
  lead?: ReactNode;
  accent?: boolean;
}) {
  return (
    <div className="pw-auth-card-head">
      <h1 id={id}>{title}</h1>
      {lead ? <p>{lead}</p> : null}
      {accent ? <span className="pw-auth-accent" aria-hidden="true" /> : null}
    </div>
  );
}

const GRID_THICK =
  'M-200 120 H920M-200 300 H920M-200 480 H920M-200 660 H920M60 -200 V920M260 -200 V920M460 -200 V920M660 -200 V920';
const GRID_THIN = 'M-200 210 H920M-200 390 H920M-200 570 H920M160 -200 V920M360 -200 V920M560 -200 V920';
const BRAND = '#0b6a5c';
const MINT = '#9ff0d3';

function NumberedStop({ x, y, n }: { x: number; y: number; n: number }) {
  return (
    <>
      <circle cx={x} cy={y} r="17" fill="#ffffff" />
      <text
        x={x}
        y={y + 6}
        textAnchor="middle"
        fontFamily="Be Vietnam Pro, sans-serif"
        fontSize="16"
        fontWeight="700"
        fill={BRAND}
      >
        {n}
      </text>
    </>
  );
}

/**
 * Drawing of the wide brand panel, exactly as the prototype: one 720×720 drawing (street grid, blobs, skyline, route
 * and pin) covering the panel. "Đăng ký cửa hàng" draws it without the thin streets, faint pins and inner halo.
 */
export function RouteArt({ variant = 'login' }: { variant?: 'login' | 'register' }) {
  const login = variant === 'login';
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
        <path d={GRID_THICK} strokeOpacity="0.09" strokeWidth="12" />
        {login ? <path d={GRID_THIN} strokeOpacity="0.05" strokeWidth="3" /> : null}
      </g>
      <g fill="#ffffff" fillOpacity="0.07">
        <rect x="520" y="560" width="26" height="160" />
        <rect x="550" y="520" width="34" height="200" />
        <rect x="588" y="590" width="22" height="130" />
        <rect x="614" y="540" width="30" height="180" />
        <rect x="648" y="600" width="24" height="120" />
        <rect x="676" y="570" width="44" height="150" />
      </g>
      {login ? (
        <g fill="#ffffff" fillOpacity="0.18">
          <path d="M386 268 s-12 -11 -12 -20 a12 12 0 0 1 24 0 c0 9 -12 20 -12 20z" />
          <path d="M676 306 s-12 -11 -12 -20 a12 12 0 0 1 24 0 c0 9 -12 20 -12 20z" />
        </g>
      ) : null}
      <path
        d="M150 500 C 190 440, 230 418, 278 418 S 380 360, 442 324 S 540 290, 543 236"
        fill="none"
        stroke="#ffffff"
        strokeWidth="5"
        strokeDasharray="12 11"
        strokeLinecap="round"
      />
      <NumberedStop x={278} y={418} n={1} />
      <NumberedStop x={442} y={324} n={2} />
      <ellipse cx="543" cy="228" rx="34" ry="11" fill="#ffffff" fillOpacity="0.18" />
      {login ? <ellipse cx="543" cy="228" rx="18" ry="6" fill="#ffffff" fillOpacity="0.35" /> : null}
      <path d="M543 224 s-36 -32 -36 -60 a36 36 0 0 1 72 0 c0 28 -36 60 -36 60z" fill="#ffffff" />
      <circle cx="543" cy="164" r="13" fill={BRAND} />
    </svg>
  );
}

/** Soft shapes behind the card: tint circles in two corners, and on "Đăng nhập" a second circle and a faint pin. */
export function MainArt({ variant = 'login' }: { variant?: 'login' | 'register' }) {
  const login = variant === 'login';
  return (
    <svg
      className="pw-auth-art"
      viewBox="0 0 600 720"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="600" cy="0" r="170" className="pw-auth-tint" fillOpacity="0.7" />
      {login ? <circle cx="600" cy="0" r="110" className="pw-auth-tint" /> : null}
      <circle cx="560" cy="760" r="200" className="pw-auth-tint" fillOpacity="0.6" />
      {login ? (
        <path
          d="M520 640 s-22 -20 -22 -37 a22 22 0 0 1 44 0 c0 17 -22 37 -22 37z"
          className="pw-auth-pin"
          fillOpacity="0.18"
        />
      ) : null}
    </svg>
  );
}

/** Drawing of the compact brand panel: the street grid and one picture in the middle (prototype). */
export function CompactArt({ picture }: { picture: 'mail' | 'lock' | 'invite' }) {
  return (
    <svg
      className="pw-auth-art"
      viewBox="0 0 720 720"
      preserveAspectRatio="xMidYMid slice"
      aria-hidden="true"
      focusable="false"
    >
      <g transform="rotate(-24 360 360)" stroke="#ffffff" fill="none" strokeOpacity="0.09" strokeWidth="12">
        <path d={GRID_THICK} />
      </g>
      {picture === 'mail' ? (
        <>
          <rect x="250" y="250" width="220" height="160" rx="16" fill="none" stroke="#ffffff" strokeWidth="6" />
          <path d="M258 262 L360 342 L462 262" fill="none" stroke="#ffffff" strokeWidth="6" strokeLinejoin="round" />
          <circle cx="470" cy="250" r="34" fill={MINT} />
          <text
            x="470"
            y="262"
            textAnchor="middle"
            fontFamily="Be Vietnam Pro, sans-serif"
            fontSize="34"
            fontWeight="700"
            fill={BRAND}
          >
            !
          </text>
        </>
      ) : null}
      {picture === 'lock' ? (
        <>
          <rect x="280" y="320" width="160" height="120" rx="16" fill="none" stroke="#ffffff" strokeWidth="6" />
          <path d="M310 320 V280 a50 50 0 0 1 100 0 V320" fill="none" stroke="#ffffff" strokeWidth="6" />
          <circle cx="360" cy="372" r="12" fill={MINT} />
          <path d="M360 384 V408" stroke={MINT} strokeWidth="6" strokeLinecap="round" />
        </>
      ) : null}
      {picture === 'invite' ? (
        <>
          <rect x="250" y="270" width="220" height="150" rx="16" fill="none" stroke="#ffffff" strokeWidth="6" />
          <path d="M250 310 H470" stroke="#ffffff" strokeWidth="6" />
          <circle cx="312" cy="365" r="22" fill="none" stroke={MINT} strokeWidth="6" />
          <path d="M352 352 H430 M352 378 H410" stroke="#ffffff" strokeWidth="6" strokeLinecap="round" />
        </>
      ) : null}
    </svg>
  );
}
