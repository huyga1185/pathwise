import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../data/api_client.dart';
import '../platform/clock.dart';
import '../platform/config.dart';
import '../platform/launcher.dart';
import '../platform/location.dart';
import '../platform/map/basemap.dart';
import '../platform/preferences.dart';
import '../platform/screen_awake.dart';
import '../platform/token_store.dart';

/// Composition root (ARCHITECTURE §2.1): the ports' implementations. Tests override these providers with fakes.
final appConfigProvider = Provider<AppConfig>((ref) => AppConfig.fromEnvironment());

final tokenStoreProvider = Provider<TokenStore>((ref) => SecureTokenStore());

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(baseUrl: ref.watch(appConfigProvider).apiBase, tokens: ref.watch(tokenStoreProvider));
});

final clockProvider = Provider<Clock>((ref) => const SystemClock());

final launcherProvider = Provider<Launcher>((ref) => const UrlLauncher());

final preferencesProvider = Provider<Preferences>((ref) => SharedPreferencesStore());

final locationSourceProvider = Provider<LocationSource>((ref) => GeolocatorLocationSource());

final screenAwakeProvider = Provider<ScreenAwake>((ref) => const WakelockScreenAwake());

/// Map tiles of the configuration (P-45); tests override it with [blankBasemap] so that no tile is fetched.
final basemapProvider = Provider<Basemap>((ref) => basemapFor(ref.watch(appConfigProvider), debug: kDebugMode));
