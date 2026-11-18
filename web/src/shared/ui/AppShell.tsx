import { createContext, useContext, useEffect, useId, useLayoutEffect, useRef, useState, type ReactNode } from 'react';
import { Link, NavLink, useLocation } from 'react-router';
import { vi } from '@/shared/i18n/vi';
import { cx } from './cx';
import { SidebarIcon } from './icons';
import { reducedMotion, useExit } from '@/shared/hooks/useExit';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';

export interface ShellNavItem {
  to: string;
  label: string;
  icon: ReactNode;
  /** Active only on this exact path, not on its children ("/admin"). */
  end?: boolean;
}

export interface ShellAccount {
  name: string;
  email: string;
  /** Store name, or the admin role: shown in the account menu. */
  context?: string;
  profileTo: string;
  onLogOut: () => void;
}

export interface AppShellProps {
  items: ShellNavItem[];
  navLabel: string;
  /** Small title at the top of the sidebar (e.g. "Quản trị"); none for the store area. */
  sidebarTitle?: string;
  account: ShellAccount;
  children: ReactNode;
}

/** True inside an AppShell: a page that can stand alone (the missing page) then draws only its content. */
const InShell = createContext(false);
export function useInAppShell(): boolean {
  return useContext(InShell);
}

/**
 * Phone: the leading end of the top bar, where a sub-page's back button goes (OWNER_UI_REDESIGN §6: "Đầu: … nút quay
 * lại (trang con)"; HIG Toolbars: "Elements that let people return to the previous document… appear at the far leading
 * edge"). Null on a wide window (its leading end is the sidebar button) and outside the shell: the back button stays
 * in the page.
 */
const TopbarLeading = createContext<HTMLElement | 'pending' | null>(null);
/** The top bar's leading element; 'pending' on a phone before it is mounted (draw nothing yet, not in the page). */
export function useTopbarLeading(): HTMLElement | 'pending' | null {
  return useContext(TopbarLeading);
}

const SIDEBAR_KEY = 'pw-sidebar';
/** Wide window: sidebar; narrower: tab bar (the same 960px step as the account screens and the lists). */
const useWide = () => useMediaQuery(WIDE_QUERY);

/**
 * Keeps an element mounted for a moment after it is closed, so it can animate out ("closing"); with reduced motion it
 * goes at once. While closing it is hidden from assistive technologies and inert.
 */
function readSidebarOpen(): boolean {
  try {
    return window.localStorage.getItem(SIDEBAR_KEY) !== 'closed';
  } catch {
    return true;
  }
}

/** First letters of the first and last words ("Chị Ngọc" → "CN", "lan@example.com" → "L"). */
export function initials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return '';
  const first = words[0]?.[0] ?? '';
  const last = words.length > 1 ? (words[words.length - 1]?.[0] ?? '') : '';
  return (first + last).toUpperCase();
}

/**
 * The navigation (sidebar or tab bar) with one highlight under the current area that slides to the new one when the
 * area changes, with a little give: the controls "continually shape shift" between sections (WWDC25 219). Measured
 * from the current link, so it follows any size; no slide on the first paint, nor with reduced motion (CSS).
 */
function NavList({
  items,
  label,
  className,
  itemClassName,
  title,
  crossFade,
}: {
  items: ShellNavItem[];
  label: string;
  className: string;
  itemClassName: string;
  title?: string;
  /** Page changes through a View Transition (wide window only, see AppShell). */
  crossFade: boolean;
}) {
  const nav = useRef<HTMLElement>(null);
  const { pathname } = useLocation();
  const [box, setBox] = useState<{ x: number; y: number; w: number; h: number } | null>(null);
  const [ready, setReady] = useState(false);

  useLayoutEffect(() => {
    const element = nav.current;
    if (!element) return undefined;
    const measure = () => {
      const active = element.querySelector<HTMLElement>('.is-active');
      setBox(
        active ? { x: active.offsetLeft, y: active.offsetTop, w: active.offsetWidth, h: active.offsetHeight } : null,
      );
    };
    measure();
    const frame = window.requestAnimationFrame(() => setReady(true));
    if (typeof ResizeObserver === 'undefined') return () => window.cancelAnimationFrame(frame);
    const observer = new ResizeObserver(measure);
    observer.observe(element);
    return () => {
      window.cancelAnimationFrame(frame);
      observer.disconnect();
    };
  }, [pathname, items]);

  return (
    <nav ref={nav} className={className} aria-label={label}>
      {title ? <p className="pw-sidebar-title">{title}</p> : null}
      {box ? (
        <span
          className={cx('pw-nav-highlight', ready && 'is-ready')}
          aria-hidden="true"
          style={{ transform: `translate(${box.x}px, ${box.y}px)`, width: box.w, height: box.h }}
        />
      ) : null}
      {items.map((item) => (
        <NavLink
          key={item.to}
          to={item.to}
          end={item.end}
          // Wide window: switching areas cross-fades the old page into the new one (ui.css "pw-page").
          viewTransition={crossFade}
          className={({ isActive }) => cx(itemClassName, isActive && 'is-active')}
        >
          <span className="pw-nav-icon" aria-hidden="true">
            {item.icon}
          </span>
          <span className="pw-nav-label">{item.label}</span>
        </NavLink>
      ))}
    </nav>
  );
}

/** How the page content comes in after a navigation, when no View Transition runs (phone, other links, other
 * browsers): between areas it fades in and rises a little, like the wide window's cross-fade; going deeper it slides in
 * from the trailing side and coming back up from the leading side (HIG Motion: "follows people's… expectations"); a
 * fade only with reduced motion. On a phone the bars are never captured for a View Transition: captured, the sticky
 * top bar flickered on a scrolled page and the tab bar lost its blur for a moment. */
function inViewTransition(): boolean {
  try {
    return document.documentElement.matches(':active-view-transition');
  } catch {
    return false;
  }
}

/** Pages that are tabs of one page: switching between them is a tab switch (only the underline moves), not a page change. */
const TAB_PAGES = [['/store', '/store/audit']];

export function pageMotion(from: string, to: string, roots: string[]): Keyframe[] | null {
  if (from === to) return null;
  if (TAB_PAGES.some((group) => group.includes(from) && group.includes(to))) return null;
  const fade: Keyframe[] = [{ opacity: 0 }, { opacity: 1 }];
  if (reducedMotion()) return fade;
  const rise: Keyframe[] = [
    { opacity: 0, transform: 'translateY(12px)' },
    { opacity: 1, transform: 'none' },
  ];
  if (roots.includes(from) && roots.includes(to)) return rise;
  if (to.startsWith(`${from}/`))
    return [
      { opacity: 0, transform: 'translateX(32px)' },
      { opacity: 1, transform: 'none' },
    ];
  if (from.startsWith(`${to}/`))
    return [
      { opacity: 0, transform: 'translateX(-32px)' },
      { opacity: 1, transform: 'none' },
    ];
  // Any other change (an area outside the menu such as "Tài khoản", one plan to another): like switching areas.
  return rise;
}

/**
 * The account button (initials) and its menu: who is signed in, the store, "Tài khoản", "Đăng xuất". One
 * button instead of the store, the name and "Đăng xuất" always on screen (HIG Toolbars: "Choose items deliberately to
 * avoid overcrowding"; P-74). A menu button: Escape, a click outside or choosing an item closes it, focus goes back to
 * the button.
 */
function AccountMenu({ account, crossFade }: { account: ShellAccount; crossFade: boolean }) {
  const [open, setOpen] = useState(false);
  // The menu pops out of the button and back into it (WWDC25 219: "the bubble simply pops open… right where you just
  // tapped"); while it closes it is no longer a menu for assistive technologies.
  const presence = useExit(open, 140);
  const menuId = useId();
  const button = useRef<HTMLButtonElement>(null);
  const menu = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return undefined;
    menu.current?.querySelector<HTMLElement>('[role="menuitem"]')?.focus();
    const close = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!menu.current?.contains(target) && !button.current?.contains(target)) setOpen(false);
    };
    document.addEventListener('pointerdown', close);
    return () => document.removeEventListener('pointerdown', close);
  }, [open]);

  const closeAndFocus = () => {
    setOpen(false);
    button.current?.focus();
  };
  const onKeyDown = (event: React.KeyboardEvent) => {
    const items = [...(menu.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [])];
    const index = items.indexOf(document.activeElement as HTMLElement);
    if (event.key === 'Escape') {
      event.preventDefault();
      closeAndFocus();
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      const step = event.key === 'ArrowDown' ? 1 : -1;
      items[(index + step + items.length) % items.length]?.focus();
    } else if (event.key === 'Tab') {
      setOpen(false);
    }
  };

  return (
    <div className="pw-account">
      <button
        ref={button}
        type="button"
        className="pw-account-button"
        aria-label={vi.shell.account(account.name)}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => setOpen((value) => !value)}
      >
        {initials(account.name)}
      </button>
      {presence !== 'closed' ? (
        <div
          ref={menu}
          id={open ? menuId : undefined}
          className={cx('pw-account-menu', !open && 'is-closing')}
          role={open ? 'menu' : undefined}
          aria-label={open ? vi.shell.accountMenu : undefined}
          aria-hidden={open ? undefined : true}
          inert={!open}
          onKeyDown={onKeyDown}
        >
          <div className="pw-account-who" role="presentation">
            <strong>{account.name}</strong>
            {account.email && account.email !== account.name ? <span>{account.email}</span> : null}
            {account.context ? <span>{account.context}</span> : null}
          </div>
          <Link
            role="menuitem"
            className="pw-account-item"
            to={account.profileTo}
            viewTransition={crossFade}
            onClick={() => setOpen(false)}
          >
            {vi.profile.menu}
          </Link>
          <button
            type="button"
            role="menuitem"
            className="pw-account-item"
            onClick={() => {
              setOpen(false);
              account.onLogOut();
            }}
          >
            {vi.auth.logout}
          </button>
        </div>
      ) : null}
    </div>
  );
}

/**
 * Frame of every signed-in page (P-74, docs/client/OWNER_UI_REDESIGN.md §6): on a wide window a sidebar on the
 * leading side that can be hidden (HIG Sidebars), on a phone a tab bar floating at the bottom (HIG Tab bars, iOS); a
 * top bar with the sidebar button and the account menu. No logo (HIG Toolbars: "Don't title windows with your app
 * name"); each page carries its own title.
 */
export function AppShell({ items, navLabel, sidebarTitle, account, children }: AppShellProps) {
  const [sidebarOpen, setSidebarOpen] = useState(readSidebarOpen);
  // One navigation at a time, so assistive technologies meet a single "Điều hướng chính".
  const wide = useWide();
  const sidebarShown = wide && sidebarOpen;
  const sidebar = useExit(sidebarShown, 200);
  const main = useRef<HTMLElement>(null);
  const { pathname } = useLocation();
  const smallTitle = useSmallTitle(main, !wide);
  const [leading, setLeading] = useState<HTMLElement | null>(null);
  const previous = useRef(pathname);
  useEffect(() => {
    const from = previous.current;
    previous.current = pathname;
    const frames = pageMotion(from, pathname, [...items.map((item) => item.to), account.profileTo]);
    const element = main.current;
    if (!frames || !element || typeof element.animate !== 'function') return;
    // A View Transition (shell links) already animates this change; this is for the other links and other browsers.
    if (inViewTransition()) return;
    const slides = frames.some((frame) => 'transform' in frame);
    element.animate(frames, { duration: slides ? 280 : 160, easing: 'cubic-bezier(0.2, 0.8, 0.2, 1)' });
  }, [pathname, items, account.profileTo]);
  const toggleSidebar = () => {
    const next = !sidebarOpen;
    setSidebarOpen(next);
    try {
      window.localStorage.setItem(SIDEBAR_KEY, next ? 'open' : 'closed');
    } catch {
      // Only remembered while the page is open.
    }
  };

  return (
    <div className={cx('pw-shell', sidebarShown && 'has-sidebar')}>
      {wide && sidebar !== 'closed' ? (
        <div
          className={cx('pw-sidebar-slot', !sidebarShown && 'is-closing')}
          inert={!sidebarShown}
          aria-hidden={sidebarShown ? undefined : true}
        >
          <NavList
            items={items}
            label={navLabel}
            className="pw-sidebar"
            itemClassName="pw-sidebar-item"
            title={sidebarTitle}
            crossFade
          />
        </div>
      ) : null}
      <div className="pw-shell-main">
        <div className={cx('pw-topbar', smallTitle && 'has-title')}>
          {wide ? (
            <button
              type="button"
              className="pw-topbar-button pw-sidebar-toggle"
              aria-label={sidebarOpen ? vi.shell.hideSidebar : vi.shell.showSidebar}
              aria-expanded={sidebarOpen}
              title={sidebarOpen ? vi.shell.hideSidebar : vi.shell.showSidebar}
              onClick={toggleSidebar}
            >
              <SidebarIcon />
            </button>
          ) : null}
          {!wide ? <div ref={setLeading} className="pw-topbar-leading" /> : null}
          {!wide ? (
            <span className={cx('pw-topbar-title', smallTitle && 'is-shown')} aria-hidden="true">
              {smallTitle}
            </span>
          ) : null}
          <AccountMenu account={account} crossFade={wide} />
        </div>
        <main ref={main} className="pw-page">
          <InShell.Provider value={true}>
            <TopbarLeading.Provider value={wide ? null : (leading ?? 'pending')}>{children}</TopbarLeading.Provider>
          </InShell.Provider>
        </main>
      </div>
      {wide ? null : (
        <NavList
          items={items}
          label={navLabel}
          className="pw-tabbar"
          itemClassName="pw-tabbar-item"
          crossFade={false}
        />
      )}
    </div>
  );
}

/**
 * Phone: the page's large title (its h1) gives way to a small one in the middle of the top bar once it scrolls under
 * the bar, and comes back when people scroll to the top (HIG Toolbars: "Use a large title to help people stay oriented
 * as they navigate and scroll. By default, a large title transitions to a standard title as people begin scrolling…
 * and transitions back to large when people scroll to the top"). Follows the h1 as pages and their content change.
 */
function useSmallTitle(main: React.RefObject<HTMLElement | null>, enabled: boolean): string | null {
  const [title, setTitle] = useState<string | null>(null);
  useEffect(() => {
    const root = main.current;
    if (!enabled || !root || typeof IntersectionObserver === 'undefined') return undefined;
    let watched: HTMLElement | null = null;
    let observer: IntersectionObserver | null = null;
    const bind = () => {
      const heading = root.querySelector<HTMLElement>('h1');
      if (heading === watched) return;
      observer?.disconnect();
      watched = heading;
      setTitle(null);
      if (!heading) return;
      const bar = (root.previousElementSibling as HTMLElement | null)?.offsetHeight ?? 60;
      observer = new IntersectionObserver(
        ([entry]) =>
          setTitle(entry && !entry.isIntersecting && entry.boundingClientRect.top < bar ? heading.textContent : null),
        { rootMargin: `-${bar}px 0px 0px 0px` },
      );
      observer.observe(heading);
    };
    bind();
    const changes = new MutationObserver(bind);
    changes.observe(root, { childList: true, subtree: true });
    return () => {
      changes.disconnect();
      observer?.disconnect();
    };
  }, [main, enabled]);
  return enabled ? title : null;
}
