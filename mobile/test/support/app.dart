import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/app/app.dart';
import 'package:pathwise_driver/app/providers.dart';
import 'package:pathwise_driver/platform/clock.dart';
import 'package:pathwise_driver/platform/launcher.dart';
import 'package:pathwise_driver/platform/token_store.dart';

import 'fake_api.dart';

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

/// The whole app (router, theme, session) on the fake server; returns the token store to inspect.
Future<MemoryTokenStore> pumpApp(
  WidgetTester tester,
  FakeServer server, {
  String? refreshToken,
  Clock? clock,
  Launcher? launcher,
}) async {
  final store = MemoryTokenStore(refreshToken: refreshToken);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        apiClientProvider.overrideWithValue(fakeClient(server, store)),
        clockProvider.overrideWithValue(clock ?? FixedClock(sampleNow)),
        launcherProvider.overrideWithValue(launcher ?? FakeLauncher()),
      ],
      child: const PathwiseApp(),
    ),
  );
  await tester.pumpAndSettle();
  return store;
}
