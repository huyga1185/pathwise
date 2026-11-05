import { createBrowserRouter, Navigate, type RouteObject } from 'react-router';
import { ProfilePage } from '@/features/account';
import { VehiclesPage } from '@/features/fleet';
import { MembersPage } from '@/features/members';
import { PlanDetailPage, PlanFormPage, PlanListPage } from '@/features/plans';
import { StorePage } from '@/features/store';
import { AcceptInvitePage, ForgotPasswordPage, LoginPage, RegisterPage, VerifyEmailPage } from '@/features/auth';
import { AppLayout } from './AppLayout';
import { RedirectIfSignedIn, RequireAuth, RequireDispatcher } from './guards';
import { DriverUseAppPage, ForbiddenPage, NotFoundPage } from './pages';

/** Routes of docs/client/SCREENS.md §1. */
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
      { path: '/plans', element: <PlanListPage /> },
      { path: '/plans/new', element: <PlanFormPage /> },
      { path: '/plans/:id', element: <PlanDetailPage /> },
      { path: '/plans/:id/edit', element: <PlanFormPage /> },
      { path: '/vehicles', element: <VehiclesPage /> },
      { path: '/members', element: <MembersPage /> },
      { path: '/store', element: <StorePage /> },
      { path: '/profile', element: <ProfilePage /> },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
];

export function createAppRouter() {
  return createBrowserRouter(routes, { basename: import.meta.env.BASE_URL.replace(/\/$/, '') });
}
