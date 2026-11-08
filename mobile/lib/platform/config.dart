/// AppConfig port (docs/client/ARCHITECTURE.md §2.1, §11): the build's settings, from `--dart-define`.
class AppConfig {
  const AppConfig({required this.apiBase, required this.webBase, required this.basemap, required this.maptilerKey});

  /// Read once from `--dart-define=API_BASE=… --dart-define=WEB_BASE=… --dart-define=BASEMAP=… --dart-define=MAPTILER_KEY=…`.
  /// The API default reaches a backend on the dev machine from the Android emulator. The web app is served next to
  /// the API under `/app` (P-47), so WEB_BASE defaults to API_BASE + `/app`; in development (Vite on its own port)
  /// pass it, e.g. `http://10.0.2.2:5173/app`.
  factory AppConfig.fromEnvironment() {
    const apiBase = String.fromEnvironment('API_BASE', defaultValue: 'http://10.0.2.2:8080');
    const webBase = String.fromEnvironment('WEB_BASE');
    return const AppConfig(
      apiBase: apiBase,
      webBase: webBase == '' ? '$apiBase/app' : webBase,
      basemap: String.fromEnvironment('BASEMAP', defaultValue: 'maptiler'),
      maptilerKey: String.fromEnvironment('MAPTILER_KEY'),
    );
  }

  /// Origin of the API; paths already start with /api/v1.
  final String apiBase;

  /// Base of the web app (`…/app`), for the screens the driver app opens in the browser (M03: forgot password,
  /// accept an invitation).
  final String webBase;
  final String basemap;
  final String maptilerKey;

  /// A page of the web app, e.g. `webPage('forgot-password')`.
  Uri webPage(String path) =>
      Uri.parse('${webBase.endsWith('/') ? webBase.substring(0, webBase.length - 1) : webBase}/$path');
}
