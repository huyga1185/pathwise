import { useSyncExternalStore, type ReactNode, type Ref } from 'react';
import { Link, Outlet } from 'react-router';
import { vi } from '@/shared/i18n/vi';
import { LogoMark, PauseIcon, PlayIcon, Wordmark } from '@/shared/ui';
import './login.css';
// The picture of "Đăng nhập" / "Đăng ký cửa hàng" (map, route, pins, city; docs/client/LIQUID_GLASS.md §9), and the
// same picture without its animations for reduced motion.
import mapRoute from './map-route.svg';
import mapRouteStill from './map-route-static.svg';

export interface AuthLayoutProps {
  /** wide: the large brand panel of "Đăng nhập" and "Đăng ký"; compact: the smaller one of the other account screens. */
  panel: 'wide' | 'compact';
  /** Decorative drawing covering the brand panel ({@link MapArt} or {@link CompactArt}). */
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
      <Wordmark onBrand />
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
          <Link to="/login" viewTransition className="pw-auth-logo" aria-label={vi.form.toLoginLabel}>
            {logo}
          </Link>
        ) : (
          <div className="pw-auth-logo">{logo}</div>
        )}
        <div className="pw-auth-hero">{pitch}</div>
      </section>
      <main className={compact ? 'pw-auth-main pw-auth-main-compact' : 'pw-auth-main'}>
        {mainArt}
        {/* Holds the card's surface on two columns (login.css), apart from the card's content, so a page change can
            keep the surface in place and fade the content alone. A plain pass-through elsewhere (display: contents). */}
        <div className="pw-auth-surface">{children}</div>
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
  headingRef,
}: {
  id: string;
  title: ReactNode;
  lead?: ReactNode;
  accent?: boolean;
  /** For a card that changes its content in place: the page moves focus to the title (tabIndex −1) after a change. */
  headingRef?: Ref<HTMLHeadingElement>;
}) {
  return (
    <div className="pw-auth-card-head">
      <h1 id={id} ref={headingRef} tabIndex={headingRef ? -1 : undefined}>
        {title}
      </h1>
      {lead ? <p>{lead}</p> : null}
      {accent ? <span className="pw-auth-accent" aria-hidden="true" /> : null}
    </div>
  );
}

const GRID_THICK =
  'M-200 120 H920M-200 300 H920M-200 480 H920M-200 660 H920M60 -200 V920M260 -200 V920M460 -200 V920M660 -200 V920';
const BRAND = '#0b6a5c';
const MINT = '#9ff0d3';

/**
 * Whether the picture's animations are paused (WCAG 2.2.2 Pause, Stop, Hide: they loop next to the form). One value for
 * the full-screen picture and the panel's, kept for the next visit; storage may be unavailable (private window), then
 * it only lasts for the page.
 */
const MOTION_KEY = 'pw.auth.mapPaused';
let mapPaused = readPaused();
const motionListeners = new Set<() => void>();
function readPaused(): boolean {
  try {
    return window.localStorage.getItem(MOTION_KEY) === '1';
  } catch {
    return false;
  }
}
function setMapPaused(paused: boolean) {
  mapPaused = paused;
  try {
    window.localStorage.setItem(MOTION_KEY, paused ? '1' : '0');
  } catch {
    // The choice still holds until the page is left.
  }
  motionListeners.forEach((listener) => listener());
}
function useMapPaused(): boolean {
  return useSyncExternalStore(
    (listener) => {
      motionListeners.add(listener);
      return () => motionListeners.delete(listener);
    },
    () => mapPaused,
  );
}

/** Pauses or plays the picture's animations: paused shows the still copy of the picture. */
function MapMotionToggle({ className }: { className: string }) {
  const paused = useMapPaused();
  const label = paused ? vi.auth.playMotion : vi.auth.pauseMotion;
  return (
    <button
      type="button"
      className={`pw-auth-motion ${className}`}
      aria-label={label}
      title={label}
      onClick={() => setMapPaused(!paused)}
    >
      {paused ? <PlayIcon /> : <PauseIcon />}
    </button>
  );
}

/**
 * The picture of the wide brand panel, with a dark veil (login.css) so the white logo and text stay readable on its light
 * greens. Reduced motion or the pause button swap in the still copy. In the panel (phone, tablet) it carries its own
 * pause button; the full-screen copy of {@link AuthStage} is decorative and the stage holds the button.
 */
export function MapArt({ toggle = true }: { toggle?: boolean }) {
  const paused = useMapPaused();
  return (
    <>
      <picture>
        <source media="(prefers-reduced-motion: reduce)" srcSet={mapRouteStill} />
        <img className="pw-auth-art pw-auth-map" src={paused ? mapRouteStill : mapRoute} alt="" decoding="sync" />
      </picture>
      <span className="pw-auth-veil" aria-hidden="true" />
      {toggle ? <MapMotionToggle className="pw-auth-motion-panel" /> : null}
    </>
  );
}

/**
 * Layout route of "Đăng nhập", "Đăng ký cửa hàng" and "Nhận lời mời": the full-screen picture of two columns lives here, once, so going
 * from one page to the other does not rebuild it — rebuilding restarted its animations (the pin and the vehicle jumped
 * back) and showed them twice during the transition. On a phone each page keeps its picture in its own panel.
 */
export function AuthStage({ children }: { children?: ReactNode }) {
  return (
    <div className="pw-auth-stage">
      <div className="pw-auth-stage-art" aria-hidden="true">
        <MapArt toggle={false} />
      </div>
      {children ?? <Outlet />}
      {/* After the page in the tab order: the form comes first. */}
      <MapMotionToggle className="pw-auth-motion-stage" />
    </div>
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
export function CompactArt({ picture }: { picture: 'mail' | 'lock' }) {
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
    </svg>
  );
}
