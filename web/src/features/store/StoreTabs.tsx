import { useEffect, useState } from 'react';
import { NavLink, useLocation } from 'react-router';
import { vi } from '@/shared/i18n/vi';
import { cx } from '@/shared/ui/cx';

/**
 * Sub-pages of "Cửa hàng" for the OWNER (W11): settings and the store's audit log. Like the members page: one tabbed
 * control, centred, on every width (owner's choice 2026-11-18, §8.9: on a Mac a tab view's control looks like a
 * segmented control on the top edge of the content, HIG Tab views; on a phone a segmented control). They are links to
 * two pages, so a navigation with `aria-current`.
 */
/** The thumb's last place: the two tabs are separate pages, so a new page's thumb slides from the old one. */
let lastOnAudit: boolean | null = null;

export function StoreTabs() {
  const { pathname } = useLocation();
  const onAudit = pathname.endsWith('/audit');
  const [thumbOnAudit, setThumbOnAudit] = useState(lastOnAudit ?? onAudit);
  useEffect(() => {
    lastOnAudit = onAudit;
    const frame = requestAnimationFrame(() => setThumbOnAudit(onAudit));
    return () => cancelAnimationFrame(frame);
  }, [onAudit]);
  return (
    <nav className="pw-segmented" style={{ '--segments': 2 } as never} aria-label={vi.storeTabs.label}>
      <span
        className="pw-segmented-thumb"
        aria-hidden="true"
        style={{ transform: `translateX(${thumbOnAudit ? 100 : 0}%)` }}
      />
      <NavLink end to="/store" className={({ isActive }) => cx('pw-segment', isActive && 'is-selected')}>
        {vi.storeTabs.info}
      </NavLink>
      <NavLink to="/store/audit" className={({ isActive }) => cx('pw-segment', isActive && 'is-selected')}>
        {vi.storeTabs.audit}
      </NavLink>
    </nav>
  );
}
