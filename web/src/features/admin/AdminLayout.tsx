import { Outlet } from 'react-router';
import { useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { useTheme } from '@/shared/theme/ThemeProvider';
import { AppHeader, Button, MoonIcon, SunIcon } from '@/shared/ui';
import './admin.css';

const NAV = [
  { to: '/admin', label: vi.admin.nav.overview, end: true },
  { to: '/admin/stores', label: vi.admin.nav.stores },
  { to: '/admin/users', label: vi.admin.nav.users },
  { to: '/admin/audit', label: vi.admin.nav.audit },
];

/** Layout of the ADMIN area (W11, prototype "Quản trị"): its own menu, no store, a "QUẢN TRỊ" badge. */
export function AdminLayout() {
  const { session, logOut } = useSession();
  const { theme, setPreference } = useTheme();
  const user = session.status === 'authenticated' ? session.user : null;
  return (
    <div className="pw-app">
      <AppHeader
        items={NAV}
        navLabel={vi.admin.nav.label}
        homeTo="/admin"
        storeName={vi.admin.badge}
        userName={user?.fullName || user?.email || ''}
        profileTo="/admin/profile"
        darkLogo={theme === 'dark'}
        actions={
          <>
            <button
              type="button"
              className="pw-icon-button"
              aria-label={vi.theme.toggle}
              onClick={() => setPreference(theme === 'dark' ? 'light' : 'dark')}
            >
              {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
            </button>
            <Button variant="quiet" onClick={() => void logOut()}>
              {vi.auth.logout}
            </Button>
          </>
        }
      />
      <main className="pw-page">
        <Outlet />
      </main>
    </div>
  );
}
