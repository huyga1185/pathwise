import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../shared/i18n/vi.dart';
import '../shared/theme/theme.dart';
import 'router.dart';
import 'theme_mode.dart';

/// The driver app: Vietnamese only, light/dark theme from the system or the driver's pick (ARCHITECTURE §8).
class PathwiseApp extends ConsumerWidget {
  const PathwiseApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return MaterialApp.router(
      title: Vi.appName,
      debugShowCheckedModeBanner: false,
      theme: pwTheme(Brightness.light),
      darkTheme: pwTheme(Brightness.dark),
      themeMode: ref.watch(themeModeProvider),
      locale: const Locale('vi'),
      supportedLocales: const [Locale('vi')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      routerConfig: ref.watch(routerProvider),
    );
  }
}
