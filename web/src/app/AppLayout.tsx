import type { ReactNode } from 'react';
import { Outlet } from 'react-router';
import { useStoreReadOnly } from '@/data/meQuery';
import { useStoreQuery } from '@/data/storeQuery';
import { useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { AppShell, ChartIcon, RouteIcon, StoreIcon, TruckIcon, UsersIcon } from '@/shared/ui';

const NAV = [
  { to: '/plans', label: vi.nav.plans, icon: <RouteIcon /> },
  { to: '/reports', label: vi.nav.reports, icon: <ChartIcon /> },
  { to: '/vehicles', label: vi.nav.vehicles, icon: <TruckIcon /> },
  { to: '/members', label: vi.nav.members, icon: <UsersIcon /> },
  { to: '/store', label: vi.nav.store, icon: <StoreIcon /> },
];

/** The dispatcher frame (sidebar or tab bar, account menu) around any content (P-74). */
export function DispatcherShell({ children }: { children: ReactNode }) {
  const { session, logOut } = useSession();
  const store = useStoreQuery();
  const lock = useStoreReadOnly();
  const user = session.status === 'authenticated' ? session.user : null;
  return (
    <AppShell
      items={NAV}
      navLabel={vi.nav.main}
      account={{
        name: user?.fullName || user?.email || '',
        email: user?.email ?? '',
        context: store.data?.name,
        profileTo: '/profile',
        onLogOut: () => void logOut(),
      }}
    >
      {lock.readOnly ? (
        <div className="pw-lock-banner" role="status">
          <strong>{vi.storeLock.banner}</strong>
          <span>{lock.reason ?? vi.storeLock.noReason}</span>
          <span className="pw-muted">{vi.storeLock.readOnly}</span>
        </div>
      ) : null}
      {children}
    </AppShell>
  );
}

/** Layout of every dispatcher page: the frame around the page of the route. */
export function AppLayout() {
  return (
    <DispatcherShell>
      <Outlet />
    </DispatcherShell>
  );
}
