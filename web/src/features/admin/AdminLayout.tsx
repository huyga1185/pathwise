import type { ReactNode } from 'react';
import { Outlet } from 'react-router';
import { useSession } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { AppShell, ListIcon, OverviewIcon, StoreIcon, UsersIcon } from '@/shared/ui';
import './admin.css';

const NAV = [
  { to: '/admin', label: vi.admin.nav.overview, end: true, icon: <OverviewIcon /> },
  { to: '/admin/stores', label: vi.admin.nav.stores, icon: <StoreIcon /> },
  { to: '/admin/users', label: vi.admin.nav.users, icon: <UsersIcon /> },
  { to: '/admin/audit', label: vi.admin.nav.audit, icon: <ListIcon /> },
];

/** The ADMIN frame (W11): the same app shell with its own menu, titled "Quản trị" (P-74). */
export function AdminShell({ children }: { children: ReactNode }) {
  const { session, logOut } = useSession();
  const user = session.status === 'authenticated' ? session.user : null;
  return (
    <AppShell
      items={NAV}
      navLabel={vi.admin.nav.label}
      sidebarTitle={vi.admin.badge}
      account={{
        name: user?.fullName || user?.email || '',
        email: user?.email ?? '',
        context: vi.admin.role,
        profileTo: '/admin/profile',
        onLogOut: () => void logOut(),
      }}
    >
      {children}
    </AppShell>
  );
}

/** Layout of the ADMIN area: the frame around the page of the route. */
export function AdminLayout() {
  return (
    <AdminShell>
      <Outlet />
    </AdminShell>
  );
}
