import { Outlet } from 'react-router';
import { useStoreReadOnly } from '@/data/meQuery';
import { useStoreQuery } from '@/data/storeQuery';
import { useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { useTheme } from '@/shared/theme/ThemeProvider';
import { AppHeader, Button, MoonIcon, SunIcon } from '@/shared/ui';

const NAV = [
  { to: '/plans', label: vi.nav.plans },
  { to: '/reports', label: vi.nav.reports },
  { to: '/vehicles', label: vi.nav.vehicles },
  { to: '/members', label: vi.nav.members },
  { to: '/store', label: vi.nav.store },
];

/** Layout of every dispatcher page: header with menu, store, user, theme switch and sign out. */
export function AppLayout() {
  const { session, logOut } = useSession();
  const { theme, setPreference } = useTheme();
  const store = useStoreQuery();
  const lock = useStoreReadOnly();
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
        {lock.readOnly ? (
          <div className="pw-lock-banner" role="status">
            <strong>{vi.storeLock.banner}</strong>
            <span>{lock.reason ?? vi.storeLock.noReason}</span>
            <span className="pw-muted">{vi.storeLock.readOnly}</span>
          </div>
        ) : null}
        <Outlet />
      </main>
    </div>
  );
}
