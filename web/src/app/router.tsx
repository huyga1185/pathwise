import { createBrowserRouter, Navigate, Outlet, type RouteObject } from 'react-router';
import {
  AcceptInvitePage,
  ForgotPasswordPage,
  LoginPage,
  RegisterPage,
  ResetPasswordPage,
  VerifyEmailPage,
} from '@/features/auth';
import { AppLayout } from './AppLayout';
import { RedirectIfSignedIn, RequireAdmin, RequireAuth, RequireDispatcher } from './guards';
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
const StoreAuditPage = lazyPage(() => import('@/features/store').then((m) => ({ default: m.StoreAuditPage })));
// The ADMIN area (W11) is its own chunk, with the chart library.
const AdminLayout = lazyPage(() => import('@/features/admin').then((m) => ({ default: m.AdminLayout })));
const AdminOverviewPage = lazyPage(() => import('@/features/admin').then((m) => ({ default: m.OverviewPage })));
const AdminStoresPage = lazyPage(() => import('@/features/admin').then((m) => ({ default: m.StoresPage })));
const AdminStoreDetailPage = lazyPage(() => import('@/features/admin').then((m) => ({ default: m.StoreDetailPage })));
const AdminUsersPage = lazyPage(() => import('@/features/admin').then((m) => ({ default: m.UsersPage })));
const AdminAuditPage = lazyPage(() => import('@/features/admin').then((m) => ({ default: m.AuditPage })));
const PlanListPage = lazyPage(() => import('@/features/plans').then((m) => ({ default: m.PlanListPage })));
const PlanFormPage = lazyPage(() => import('@/features/plans').then((m) => ({ default: m.PlanFormPage })));
const ReportsPage = lazyPage(() => import('@/features/reports').then((m) => ({ default: m.ReportsPage })));
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
  { path: '/reset-password', element: <ResetPasswordPage /> },
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
      { path: '/reports', lazy: ReportsPage },
      { path: '/vehicles', lazy: VehiclesPage },
      { path: '/members', lazy: MembersPage },
      { path: '/store', lazy: StorePage },
      { path: '/store/audit', lazy: StoreAuditPage },
      { path: '/profile', lazy: ProfilePage },
    ],
  },
  {
    path: '/admin',
    element: (
      <RequireAuth>
        <RequireAdmin>
          <Outlet />
        </RequireAdmin>
      </RequireAuth>
    ),
    children: [
      {
        lazy: AdminLayout,
        children: [
          { index: true, lazy: AdminOverviewPage },
          { path: 'stores', lazy: AdminStoresPage },
          { path: 'stores/:id', lazy: AdminStoreDetailPage },
          { path: 'users', lazy: AdminUsersPage },
          { path: 'audit', lazy: AdminAuditPage },
          { path: 'profile', lazy: ProfilePage },
        ],
      },
    ],
  },
  { path: '*', element: <NotFoundPage /> },
];

export function createAppRouter() {
  return createBrowserRouter(routes, { basename: import.meta.env.BASE_URL.replace(/\/$/, '') });
}
