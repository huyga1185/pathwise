import { useQuery } from '@tanstack/react-query';
import { Outlet } from 'react-router';
import { useApi } from '@/data/ApiContext';
import { unwrap } from '@/data/http';
import { queryKeys } from '@/data/queryKeys';
import { useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { useTheme } from '@/shared/theme/ThemeProvider';
import { AppHeader, Button, MoonIcon, SunIcon } from '@/shared/ui';

const NAV = [
  { to: '/plans', label: vi.nav.plans },
  { to: '/vehicles', label: vi.nav.vehicles },
  { to: '/members', label: vi.nav.members },
  { to: '/store', label: vi.nav.store },
];

/** Layout of every dispatcher page: header with menu, store, user, theme switch and sign out. */
export function AppLayout() {
  const api = useApi();
  const { session, logOut } = useSession();
  const { theme, setPreference } = useTheme();
  const store = useQuery({ queryKey: queryKeys.store, queryFn: () => unwrap(api.GET('/api/v1/store')) });
  const user = session.status === 'authenticated' ? session.user : null;
  return (
    <div className="pw-app">
      <AppHeader
        items={NAV}
        navLabel={vi.nav.main}
        homeTo="/plans"
        storeName={store.data?.name}
        userName={user?.fullName || user?.email || ''}
        profileTo="/profile"
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
