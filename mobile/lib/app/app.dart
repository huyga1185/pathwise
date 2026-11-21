import 'package:flutter/material.dart';
import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../features/auth/auth.dart';
import '../shared/i18n/vi.dart';
import '../shared/theme/theme.dart';
import '../shared/ui/ui.dart';
import 'providers.dart';
import 'router.dart';

/// The driver app: Vietnamese only, light/dark theme from the system (ARCHITECTURE §8; HIG Dark Mode: "Avoid offering an
/// app-specific appearance setting", P-73).
class PathwiseApp extends ConsumerWidget {
  const PathwiseApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    // The system splash stays until the session is known (D2), then goes once the first screen (login, routes) is
    // drawn, so the driver never sees a second splash.
    ref.listen<Session>(sessionProvider, (_, next) {
      if (next is! SessionLoading) _dismissSplashAfterFrame(ref);
    });
    if (ref.read(sessionProvider) is! SessionLoading) _dismissSplashAfterFrame(ref);
    return MaterialApp.router(
      title: Vi.appName,
      debugShowCheckedModeBanner: false,
      theme: pwTheme(Brightness.light),
      darkTheme: pwTheme(Brightness.dark),
      themeMode: ThemeMode.system,
      locale: const Locale('vi'),
      supportedLocales: const [Locale('vi')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      routerConfig: ref.watch(routerProvider),
      // Status bar icons follow the theme on screens without an app bar (no access, loading); app bars and the login
      // header set their own on top. Without it, the light icons of the login header stayed on the light screen after
      // it (D2).
      builder: (context, child) => AnnotatedRegion<SystemUiOverlayStyle>(
        value: statusBarFor(Theme.of(context).brightness == Brightness.dark ? Brightness.light : Brightness.dark),
        child: child!,
      ),
    );
  }

  void _dismissSplashAfterFrame(WidgetRef ref) {
    final splash = ref.read(systemSplashProvider);
    // Two frames: the router redirects on the first, the new screen is painted on the second.
    SchedulerBinding.instance.addPostFrameCallback((_) {
      SchedulerBinding.instance.addPostFrameCallback((_) => splash.dismiss());
      SchedulerBinding.instance.scheduleFrame();
    });
  }
}
