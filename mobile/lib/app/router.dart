import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/account/account.dart';
import '../features/auth/auth.dart';
import '../features/driver_routes/driver_routes.dart';
import '../features/navigation/navigation.dart';
import '../shared/i18n/vi.dart';

/// Routes of the driver app (SCREENS §2), redirected by the session (ARCHITECTURE §9): loading → splash,
/// anonymous → login, not a driver → no access, signed in → today's routes. `/routes/{routeId}` opens a route straight
/// away (later the `pathwise://routes/{routeId}` deep link of notifications); `/routes/{routeId}/stops/{stopId}` reports
/// a stop's result (M02); `/routes/{routeId}/navigate?stopId=` gives turn-by-turn directions (M05); `/profile` is
/// "Thông tin của tôi" (M06).
final routerProvider = Provider<GoRouter>((ref) {
  // Redirects run again only when where the session leads changes: a renamed driver (M06) is still signed in, and a
  // refresh then raced the pop of "Thông tin của tôi" and kept it open.
  final refresh = ValueNotifier<_Access>(_accessOf(ref.read(sessionProvider)));
  ref
    ..listen<_Access>(sessionProvider.select(_accessOf), (_, next) => refresh.value = next)
    ..onDispose(refresh.dispose);
  return GoRouter(
    initialLocation: '/routes',
    refreshListenable: refresh,
    redirect: (context, state) {
      final session = ref.read(sessionProvider);
      final at = state.matchedLocation;
      return switch (session) {
        SessionLoading() => at == '/splash' ? null : '/splash',
        SessionAnonymous() => at == '/login' ? null : '/login',
        SessionSignedIn(:final user) when !user.drives => at == '/no-access' ? null : '/no-access',
        SessionSignedIn() => (at == '/login' || at == '/splash' || at == '/no-access') ? '/routes' : null,
      };
    },
    routes: [
      GoRoute(path: '/splash', builder: (context, state) => const _Splash()),
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(path: '/no-access', builder: (context, state) => const NoAccessScreen()),
      // "Thông tin của tôi" (M06), pushed over the routes from the account sheet.
      GoRoute(path: '/profile', builder: (context, state) => const ProfileScreen()),
      GoRoute(
        path: '/routes',
        builder: (context, state) => const RouteListScreen(),
        routes: [
          GoRoute(
            path: ':routeId',
            redirect: (context, state) => int.tryParse(state.pathParameters['routeId']!) == null ? '/routes' : null,
            builder: (context, state) => RouteDetailScreen(routeId: int.parse(state.pathParameters['routeId']!)),
            routes: [
              GoRoute(
                path: 'navigate',
                builder: (context, state) => NavigationScreen(
                  routeId: int.parse(state.pathParameters['routeId']!),
                  stopId: int.tryParse(state.uri.queryParameters['stopId'] ?? ''),
                ),
              ),
              GoRoute(
                path: 'stops/:stopId',
                redirect: (context, state) => int.tryParse(state.pathParameters['stopId']!) == null
                    ? '/routes/${state.pathParameters['routeId']}'
                    : null,
                builder: (context, state) => StopReportScreen(
                  routeId: int.parse(state.pathParameters['routeId']!),
                  stopId: int.parse(state.pathParameters['stopId']!),
                ),
              ),
            ],
          ),
        ],
      ),
    ],
  );
});

enum _Access { loading, anonymous, noAccess, driver }

_Access _accessOf(Session session) => switch (session) {
  SessionLoading() => _Access.loading,
  SessionAnonymous() => _Access.anonymous,
  SessionSignedIn(:final user) when !user.drives => _Access.noAccess,
  SessionSignedIn() => _Access.driver,
};

class _Splash extends StatelessWidget {
  const _Splash();

  @override
  Widget build(BuildContext context) => const Scaffold(
    body: Center(child: CircularProgressIndicator(semanticsLabel: Vi.loading)),
  );
}
