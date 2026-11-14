import '../config.dart';

/// Basemap port (P-45, ARCHITECTURE §2.1, §7): where map tiles come from, as on the web
/// (`web/src/platform/map/basemap.ts`). Screens never name a provider; tests use [blankBasemap].
class Basemap {
  const Basemap({
    required this.id,
    required this.tileUrl,
    required this.attribution,
    required this.maxZoom,
    required this.tileSize,
  });

  final String id;

  /// URL template with {z}/{x}/{y}; empty for no tiles.
  final String tileUrl;

  /// Text the provider requires, shown on every map.
  final String attribution;
  final int maxZoom;

  /// 256 or 512 pixels.
  final int tileSize;
}

const _osmCopyright = '© OpenStreetMap contributors';

/// MapTiler raster tiles (P-45). The key ships in the app: restrict it to the app's bundle id in MapTiler.
Basemap maptiler(String key, {String style = 'streets-v2'}) => Basemap(
  id: 'maptiler-$style',
  tileUrl:
      'https://api.maptiler.com/maps/${Uri.encodeComponent(style)}/{z}/{x}/{y}.png?key=${Uri.encodeComponent(key)}',
  attribution: '© MapTiler $_osmCopyright',
  maxZoom: 20,
  tileSize: 512,
);

/// The OpenStreetMap tile server: development only (its usage policy forbids production apps).
Basemap osm() => const Basemap(
  id: 'osm',
  tileUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: _osmCopyright,
  maxZoom: 19,
  tileSize: 256,
);

/// No tiles: tests, and a release build without a MapTiler key (the route still shows, on a plain background).
Basemap blankBasemap([String attribution = '']) =>
    Basemap(id: 'blank', tileUrl: '', attribution: attribution, maxZoom: 19, tileSize: 256);

/// The basemap of the configuration: `maptiler` needs `MAPTILER_KEY`; without it a debug build falls back to [osm]
/// and a release build shows no tiles. `osm` is accepted in debug builds only.
Basemap basemapFor(AppConfig config, {required bool debug}) {
  if (config.basemap == 'osm' && debug) return osm();
  if (config.basemap == 'maptiler' && config.maptilerKey.isNotEmpty) return maptiler(config.maptilerKey);
  return debug ? osm() : blankBasemap(_osmCopyright);
}
