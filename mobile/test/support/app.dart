import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:pathwise_driver/app/app.dart';
import 'package:pathwise_driver/app/providers.dart';
import 'package:pathwise_driver/platform/token_store.dart';

import 'fake_api.dart';

/// The whole app (router, theme, session) on the fake server; returns the token store to inspect.
Future<MemoryTokenStore> pumpApp(WidgetTester tester, FakeServer server, {String? refreshToken}) async {
  final store = MemoryTokenStore(refreshToken: refreshToken);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [apiClientProvider.overrideWithValue(fakeClient(server, store))],
      child: const PathwiseApp(),
    ),
  );
  await tester.pumpAndSettle();
  return store;
}
