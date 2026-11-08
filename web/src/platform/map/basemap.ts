/**
 * Basemap port (P-45, docs/client/ARCHITECTURE.md §2.1, §7): where map tiles come from. Screens never name a provider;
 * the composition root picks one from the configuration, tests use a blank one.
 */
export interface Basemap {
  id: string;
  /** Leaflet URL template with {z}/{x}/{y}. */
  tileUrl: string;
  /** Attribution the provider requires, shown on every map (HTML from this file only, never from data). */
  attribution: string;
  maxZoom: number;
  /** 256 or 512 pixels. */
  tileSize: number;
}

const OSM_COPYRIGHT =
  '<a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">© OpenStreetMap contributors</a>';

/** MapTiler raster tiles (P-45). The key is public by nature; restrict it to the app's domain in MapTiler. */
export function maptiler(key: string, style = 'streets-v2'): Basemap {
  return {
    id: `maptiler-${style}`,
    tileUrl: `https://api.maptiler.com/maps/${encodeURIComponent(style)}/{z}/{x}/{y}.png?key=${encodeURIComponent(key)}`,
    attribution: `<a href="https://www.maptiler.com/copyright/" target="_blank" rel="noreferrer">© MapTiler</a> ${OSM_COPYRIGHT}`,
    maxZoom: 20,
    tileSize: 512,
  };
}

/** The OpenStreetMap tile server: development only (its usage policy forbids production apps). */
export function osm(): Basemap {
  return {
    id: 'osm',
    tileUrl: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    attribution: OSM_COPYRIGHT,
    maxZoom: 19,
    tileSize: 256,
  };
}

/** No tiles: tests, and a production build without a MapTiler key (markers still show, on a plain background). */
export function blankBasemap(attribution = ''): Basemap {
  return { id: 'blank', tileUrl: '', attribution, maxZoom: 19, tileSize: 256 };
}

/**
 * The basemap of the configuration: `maptiler` needs `VITE_MAPTILER_KEY`; without it development falls back to
 * `osm()` and a production build shows no tiles. `osm` is accepted in development only.
 */
export function basemapFor(config: { basemap: string; maptilerKey: string }, development: boolean): Basemap {
  if (config.basemap === 'osm' && development) return osm();
  if (config.basemap === 'maptiler' && config.maptilerKey) return maptiler(config.maptilerKey);
  if (development) {
    console.warn('Map: VITE_MAPTILER_KEY is not set, using the OpenStreetMap tiles (development only).');
    return osm();
  }
  console.error(
    `Map: basemap "${config.basemap}" is not usable here; set VITE_BASEMAP=maptiler and VITE_MAPTILER_KEY.`,
  );
  return blankBasemap();
}
