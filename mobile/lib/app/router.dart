import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/auth/auth.dart';
import '../features/driver_routes/driver_routes.dart';
import '../shared/i18n/vi.dart';

/// Routes of the driver app (SCREENS §2), redirected by the session (ARCHITECTURE §9): loading → splash,
/// anonymous → login, not a driver → no access, signed in → today's routes. `/routes/{routeId}` opens a route straight
/// away (later the `pathwise://routes/{routeId}` deep link of notifications); `/routes/{routeId}/stops/{stopId}` reports
/// a stop's result (M02).
final routerProvider = Provider<GoRouter>((ref) {
  final refresh = ValueNotifier<Session>(ref.read(sessionProvider));
  ref
    ..listen<Session>(sessionProvider, (_, next) => refresh.value = next)
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

class _Splash extends StatelessWidget {
  const _Splash();

  @override
  Widget build(BuildContext context) => const Scaffold(
    body: Center(child: CircularProgressIndicator(semanticsLabel: Vi.loading)),
  );
}
