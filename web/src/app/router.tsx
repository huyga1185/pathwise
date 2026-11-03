import { createBrowserRouter, Navigate, type RouteObject } from 'react-router';
import { LoginPage } from '@/features/auth';
import { vi } from '@/shared/i18n/vi';
import { AppLayout } from './AppLayout';
import { RedirectIfSignedIn, RequireAuth, RequireDispatcher } from './guards';
import { ComingSoonPage, DriverUseAppPage, ForbiddenPage, NotFoundPage } from './pages';

/** Routes of docs/client/SCREENS.md §1. Screens of later tasks are placeholders until then. */
export const routes: RouteObject[] = [
  {
    path: '/login',
    element: (
      <RedirectIfSignedIn>
        <LoginPage />
      </RedirectIfSignedIn>
    ),
  },
  { path: '/register', element: <ComingSoonStandalone title="Đăng ký cửa hàng" /> },
  { path: '/verify-email', element: <ComingSoonStandalone title="Xác thực email" /> },
  { path: '/forgot-password', element: <ComingSoonStandalone title="Quên mật khẩu" /> },
  { path: '/invite', element: <ComingSoonStandalone title="Nhận lời mời" /> },
  {
    path: '/driver',
    element: (
      <RequireAuth>
        <DriverUseAppPage />
      </RequireAuth>
    ),
  },
  { path: '/forbidden', element: <ForbiddenPage /> },
  {
    element: (
      <RequireAuth>
        <RequireDispatcher>
          <AppLayout />
        </RequireDispatcher>
      </RequireAuth>
    ),
    children: [
      { path: '/', element: <Navigate to="/plans" replace /> },
      { path: '/plans', element: <ComingSoonPage title={vi.nav.plans} task="W05" /> },
      { path: '/vehicles', element: <ComingSoonPage title={vi.nav.vehicles} task="W03" /> },
      { path: '/members', element: <ComingSoonPage title={vi.nav.members} task="W03" /> },
      { path: '/store', element: <ComingSoonPage title={vi.nav.store} task="W04" /> },
      { path: '/profile', element: <ComingSoonPage title="Hồ sơ và bảo mật" task="W02" /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
];

function ComingSoonStandalone({ title }: { title: string }) {
  return (
    <main className="pw-standalone">
      <ComingSoonPage title={title} task="W02" />
    </main>
  );
}

export function createAppRouter() {
  return createBrowserRouter(routes, { basename: import.meta.env.BASE_URL.replace(/\/$/, '') });
}
