/// AppConfig port (docs/client/ARCHITECTURE.md §2.1, §11): the build's settings, from `--dart-define`.
class AppConfig {
  const AppConfig({required this.apiBase, required this.basemap, required this.maptilerKey});

  /// Read once from `--dart-define=API_BASE=… --dart-define=BASEMAP=… --dart-define=MAPTILER_KEY=…`.
  /// The default reaches a backend on the dev machine from the Android emulator.
  factory AppConfig.fromEnvironment() => const AppConfig(
    apiBase: String.fromEnvironment('API_BASE', defaultValue: 'http://10.0.2.2:8080'),
    basemap: String.fromEnvironment('BASEMAP', defaultValue: 'maptiler'),
    maptilerKey: String.fromEnvironment('MAPTILER_KEY'),
  );

  /// Origin of the API; paths already start with /api/v1.
  final String apiBase;
  final String basemap;
  final String maptilerKey;
}
