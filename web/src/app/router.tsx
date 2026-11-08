import { createBrowserRouter, Navigate, type RouteObject } from 'react-router';
import { AcceptInvitePage, ForgotPasswordPage, LoginPage, RegisterPage, VerifyEmailPage } from '@/features/auth';
import { AppLayout } from './AppLayout';
import { RedirectIfSignedIn, RequireAuth, RequireDispatcher } from './guards';
import { DriverUseAppPage, ForbiddenPage, NotFoundPage } from './pages';

/**
 * Screens behind the sign-in load on demand (W07): each feature, and Leaflet with the screens that draw a map, is its
 * own chunk. The account screens stay in the main chunk: the session and guards use that feature anyway.
 */
const lazyPage = (load: () => Promise<{ default: () => React.JSX.Element }>) => async () => ({
  Component: (await load()).default,
});
const ProfilePage = lazyPage(() => import('@/features/account').then((m) => ({ default: m.ProfilePage })));
const VehiclesPage = lazyPage(() => import('@/features/fleet').then((m) => ({ default: m.VehiclesPage })));
const MembersPage = lazyPage(() => import('@/features/members').then((m) => ({ default: m.MembersPage })));
const StorePage = lazyPage(() => import('@/features/store').then((m) => ({ default: m.StorePage })));
const PlanListPage = lazyPage(() => import('@/features/plans').then((m) => ({ default: m.PlanListPage })));
const PlanFormPage = lazyPage(() => import('@/features/plans').then((m) => ({ default: m.PlanFormPage })));
const PlanDetailPage = lazyPage(() => import('@/features/plans').then((m) => ({ default: m.PlanDetailPage })));

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
      { path: '/plans', lazy: PlanListPage },
      { path: '/plans/new', lazy: PlanFormPage },
      { path: '/plans/:id', lazy: PlanDetailPage },
      { path: '/plans/:id/edit', lazy: PlanFormPage },
      { path: '/vehicles', lazy: VehiclesPage },
      { path: '/members', lazy: MembersPage },
      { path: '/store', lazy: StorePage },
      { path: '/profile', lazy: ProfilePage },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
];

export function createAppRouter() {
  return createBrowserRouter(routes, { basename: import.meta.env.BASE_URL.replace(/\/$/, '') });
}
