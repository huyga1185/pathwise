import type { ReactNode } from 'react';
import { NavLink, Link } from 'react-router';
import { LogoMark } from './icons';

export interface NavItem {
  to: string;
  label: string;
}

export interface AppHeaderProps {
  items: NavItem[];
  navLabel: string;
  homeTo: string;
  storeName?: string;
  userName: string;
  profileTo: string;
  /** Extra controls on the right (theme switch, log out). */
  actions?: ReactNode;
  darkLogo: boolean;
}

/** Header of every signed-in page (prototype): logo → home, main menu with the current page marked, store and user. */
export function AppHeader({
  items,
  navLabel,
  homeTo,
  storeName,
  userName,
  profileTo,
  actions,
  darkLogo,
}: AppHeaderProps) {
  return (
    <header className="pw-header">
      <div className="pw-header-inner">
        <Link to={homeTo} className="pw-header-brand" aria-label="PathWise — trang chính">
          <LogoMark white={darkLogo} dashes={false} />
          <span>PathWise</span>
        </Link>
        <nav aria-label={navLabel} className="pw-header-nav">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => (isActive ? 'pw-nav-link pw-nav-link-active' : 'pw-nav-link')}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="pw-header-side">
          {storeName ? <span className="pw-header-store">{storeName}</span> : null}
          <Link to={profileTo} className="pw-header-user">
            {userName}
          </Link>
          {actions}
        </div>
      </div>
    </header>
  );
}
