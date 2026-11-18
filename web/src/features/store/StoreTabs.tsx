import { useEffect, useRef, useState } from 'react';
import { NavLink, useLocation } from 'react-router';
import { useTabUnderline } from '@/shared/hooks/useTabUnderline';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import { cx } from '@/shared/ui/cx';

/**
 * Sub-pages of "Cửa hàng" for the OWNER (W11): settings and the store's audit log. Like the members page (owner's
 * choice Q6): a tab view on a wide window (HIG Tab views), a segmented control on a phone (HIG Segmented controls).
 * They are links to two pages, so a navigation with `aria-current`, drawn either way.
 */
/** The phone thumb's last place: the two tabs are separate pages, so a new page's thumb slides from the old one. */
let lastOnAudit: boolean | null = null;

export function StoreTabs() {
  const wide = useMediaQuery(WIDE_QUERY);
  const { pathname } = useLocation();
  const onAudit = pathname.endsWith('/audit');
  const [thumbOnAudit, setThumbOnAudit] = useState(lastOnAudit ?? onAudit);
  useEffect(() => {
    lastOnAudit = onAudit;
    const frame = requestAnimationFrame(() => setThumbOnAudit(onAudit));
    return () => cancelAnimationFrame(frame);
  }, [onAudit]);
  const bar = useRef<HTMLElement>(null);
  const underline = useRef<HTMLSpanElement>(null);
  useTabUnderline(bar, underline, `${pathname}-${wide}`, undefined, 'store');
  const tab = wide ? 'pw-store-tab' : 'pw-segment';
  return (
    <nav
      ref={bar}
      className={wide ? 'pw-store-tabs' : 'pw-segmented'}
      style={wide ? undefined : ({ '--segments': 2 } as never)}
      aria-label={vi.storeTabs.label}
    >
      {wide ? (
        <span ref={underline} className="pw-tabs-underline" aria-hidden="true" />
      ) : (
        <span
          className="pw-segmented-thumb"
          aria-hidden="true"
          style={{ transform: `translateX(${thumbOnAudit ? 100 : 0}%)` }}
        />
      )}
      <NavLink end to="/store" className={({ isActive }) => cx(tab, isActive && (wide ? 'is-active' : 'is-selected'))}>
        {vi.storeTabs.info}
      </NavLink>
      <NavLink
        to="/store/audit"
        className={({ isActive }) => cx(tab, isActive && (wide ? 'is-active' : 'is-selected'))}
      >
        {vi.storeTabs.audit}
      </NavLink>
    </nav>
  );
}
