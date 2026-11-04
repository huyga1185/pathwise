import { createBrowserRouter, Navigate, type RouteObject } from 'react-router';
import { ProfilePage } from '@/features/account';
import { VehiclesPage } from '@/features/fleet';
import { MembersPage } from '@/features/members';
import { AcceptInvitePage, ForgotPasswordPage, LoginPage, RegisterPage, VerifyEmailPage } from '@/features/auth';
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
  { path: '/register', element: <RegisterPage /> },
  { path: '/verify-email', element: <VerifyEmailPage /> },
  { path: '/forgot-password', element: <ForgotPasswordPage /> },
  { path: '/invite', element: <AcceptInvitePage /> },
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
      { path: '/vehicles', element: <VehiclesPage /> },
      { path: '/members', element: <MembersPage /> },
      { path: '/store', element: <ComingSoonPage title={vi.nav.store} task="W04" /> },
      { path: '/profile', element: <ProfilePage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
];

export function createAppRouter() {
  return createBrowserRouter(routes, { basename: import.meta.env.BASE_URL.replace(/\/$/, '') });
}
