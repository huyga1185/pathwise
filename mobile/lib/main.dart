import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:timezone/data/latest_10y.dart' as tz_data;

import 'app/app.dart';
import 'app/providers.dart';
import 'platform/preferences.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // Planned times show in the plan's timezone (P-50); the database is bundled, nothing is fetched.
  tz_data.initializeTimeZones();
  final preferences = SharedPreferencesStore();
  runApp(ProviderScope(overrides: [preferencesProvider.overrideWithValue(preferences)], child: const PathwiseApp()));
}
