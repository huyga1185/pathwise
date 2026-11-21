import { vi } from '@/shared/i18n/vi';

/** Fade-out of the old screen; the new one fades in over this much more (app.css, `pw-scene-*`). */
const LEAVE_MS = 260;
const ENTER_MS = 500;
/** The activity indicator only shows when the new screen takes longer than this, so a quick change does not flash it. */
const SPINNER_DELAY_MS = 120;
/** Never wait longer than this for the new screen; it then just appears. */
const READY_LIMIT_MS = 2000;

const wait = (ms: number) => new Promise<void>((resolve) => window.setTimeout(resolve, ms));

/** Waits until the new screen is in place (timers: they keep running while React builds the page). */
async function whenReady(ready: () => boolean): Promise<void> {
  const start = performance.now();
  while (!ready() && performance.now() - start < READY_LIMIT_MS) await wait(16);
}

/**
 * Apple's small activity indicator (eight spokes turning in steps, HIG Progress indicators: "small and unobtrusive", no
 * text next to it). Its turning is a CSS transform the browser animates off the main thread, so it keeps moving while
 * the page is being built. Outside React: it covers the moment when neither screen is there.
 */
function showActivity(): () => void {
  const element = document.createElement('div');
  element.className = 'pw-activity';
  element.setAttribute('role', 'status');
  element.setAttribute('aria-label', vi.pages.loading);
  element.innerHTML =
    '<svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true">' +
    Array.from(
      { length: 8 },
      (_, index) =>
        `<rect x="11" y="2" width="2" height="6" rx="1" fill="currentColor" opacity="${(0.25 + (index / 8) * 0.75).toFixed(2)}" transform="rotate(${index * 45} 12 12)"/>`,
    ).join('') +
    '</svg>';
  document.body.appendChild(element);
  return () => element.remove();
}

/**
 * Signing in or out swaps the whole screen (account screens ↔ app shell) through a state change, not a link. The old
 * screen fades out, the change is applied, and the new screen fades in and rises a little once it is drawn; if that
 * takes a moment, a small activity indicator turns in the middle meanwhile (HIG Loading: "Show something as soon as
 * possible"). Nothing freezes the screen: a View Transition here held a still picture while the page was built, which
 * read as a stutter. Reduced motion: no fades, the indicator only.
 */
export async function changeScene(apply: () => void, ready: () => boolean): Promise<void> {
  const root = document.documentElement;
  const still = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
  if (!still) {
    root.classList.add('pw-scene-leaving');
    await wait(LEAVE_MS);
  }
  apply();
  let hideActivity: (() => void) | null = null;
  const timer = window.setTimeout(() => {
    hideActivity = showActivity();
  }, SPINNER_DELAY_MS);
  await whenReady(ready);
  window.clearTimeout(timer);
  (hideActivity as (() => void) | null)?.();
  root.classList.remove('pw-scene-leaving');
  if (still) return;
  root.classList.add('pw-scene-entering');
  await wait(ENTER_MS);
  root.classList.remove('pw-scene-entering');
}

/** The app shell is on screen (after signing in). */
export const appShown = () => document.querySelector('.pw-shell main.pw-page > *') !== null;
/** An account screen is on screen (after signing out). */
export const accountScreenShown = () => document.querySelector('.pw-auth .pw-auth-card') !== null;
