import 'dart:async';

import 'package:timezone/data/latest_10y.dart' as tz_data;

/// Runs before every test file: the time zone database, as main.dart loads it.
Future<void> testExecutable(FutureOr<void> Function() testMain) async {
  tz_data.initializeTimeZones();
  await testMain();
}
