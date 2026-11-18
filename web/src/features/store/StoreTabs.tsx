import { NavLink } from 'react-router';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import { cx } from '@/shared/ui/cx';

/**
 * Sub-pages of "Cửa hàng" for the OWNER (W11): settings and the store's audit log. Like the members page (owner's
 * choice Q6): a tab view on a wide window (HIG Tab views), a segmented control on a phone (HIG Segmented controls).
 * They are links to two pages, so a navigation with `aria-current`, drawn either way.
 */
export function StoreTabs() {
  const wide = useMediaQuery(WIDE_QUERY);
  const tab = wide ? 'pw-store-tab' : 'pw-segment';
  return (
    <nav
      className={wide ? 'pw-store-tabs' : 'pw-segmented'}
      style={wide ? undefined : ({ '--segments': 2 } as never)}
      aria-label={vi.storeTabs.label}
    >
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
