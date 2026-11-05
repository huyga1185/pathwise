import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/auth/auth.dart';
import '../features/routes/routes_screen.dart';
import '../shared/i18n/vi.dart';

/// Routes of the driver app (SCREENS §2), redirected by the session (ARCHITECTURE §9): loading → splash,
/// anonymous → login, not a driver → no access, signed in → today's routes.
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
      GoRoute(path: '/routes', builder: (context, state) => const RoutesScreen()),
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
