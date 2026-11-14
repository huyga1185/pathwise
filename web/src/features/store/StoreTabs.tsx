import { NavLink } from 'react-router';
import { vi } from '@/shared/i18n/vi';

/** Sub-pages of "Cửa hàng" for the OWNER (W11): settings and the store's audit log. */
export function StoreTabs() {
  return (
    <nav className="pw-store-tabs" aria-label={vi.storeTabs.label}>
      <NavLink end to="/store" className={({ isActive }) => (isActive ? 'pw-store-tab is-active' : 'pw-store-tab')}>
        {vi.storeTabs.info}
      </NavLink>
      <NavLink to="/store/audit" className={({ isActive }) => (isActive ? 'pw-store-tab is-active' : 'pw-store-tab')}>
        {vi.storeTabs.audit}
      </NavLink>
    </nav>
  );
}
