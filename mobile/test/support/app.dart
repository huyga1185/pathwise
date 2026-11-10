import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/app/app.dart';
import 'package:pathwise_driver/app/providers.dart';
import 'package:pathwise_driver/platform/clock.dart';
import 'package:pathwise_driver/platform/launcher.dart';
import 'package:pathwise_driver/platform/location.dart';
import 'package:pathwise_driver/platform/preferences.dart';
import 'package:pathwise_driver/platform/token_store.dart';

import 'fake_api.dart';
import 'location.dart';

/// 2026-12-01 09:00 in Đà Nẵng: the day of the plan in docs/ux/samples.
final sampleNow = DateTime.utc(2026, 12, 1, 2);

class FixedClock implements Clock {
  FixedClock(this.instant);

  final DateTime instant;

  @override
  DateTime now() => instant;
}

/// Records what the app asked to open instead of leaving it.
class FakeLauncher implements Launcher {
  final opened = <Uri>[];
  bool succeeds = true;

  @override
  Future<bool> open(Uri uri) async {
    opened.add(uri);
    return succeeds;
  }
}

/// Preferences of a driver who chose not to share their location (T32): screens that are not about sharing open
/// without the question. Tests of sharing pass their own [MemoryPreferences].
MemoryPreferences notSharing() => MemoryPreferences({'pw-location-sharing': 'NEVER'});

/// The whole app (router, theme, session) on the fake server; returns the token store to inspect.
Future<MemoryTokenStore> pumpApp(
  WidgetTester tester,
  FakeServer server, {
  String? refreshToken,
  Clock? clock,
  Launcher? launcher,
  Preferences? preferences,
  LocationSource? location,
}) async {
  final store = MemoryTokenStore(refreshToken: refreshToken);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        apiClientProvider.overrideWithValue(fakeClient(server, store)),
        clockProvider.overrideWithValue(clock ?? FixedClock(sampleNow)),
        launcherProvider.overrideWithValue(launcher ?? FakeLauncher()),
        preferencesProvider.overrideWithValue(preferences ?? notSharing()),
        locationSourceProvider.overrideWithValue(location ?? FakeLocationSource()),
      ],
      child: const PathwiseApp(),
    ),
  );
  await tester.pumpAndSettle();
  return store;
}
