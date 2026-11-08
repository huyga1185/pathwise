import { afterEach, describe, expect, it, vi } from 'vitest';
import { basemapFor, blankBasemap, maptiler, osm } from './basemap';

describe('Basemap', () => {
  afterEach(() => vi.restoreAllMocks());

  it('maptiler dựng URL tile 512px có key và attribution MapTiler + OSM', () => {
    const basemap = maptiler('abc 123');
    expect(basemap.tileUrl).toBe('https://api.maptiler.com/maps/streets-v2/{z}/{x}/{y}.png?key=abc%20123');
    expect(basemap.tileSize).toBe(512);
    expect(basemap.attribution).toContain('© MapTiler');
    expect(basemap.attribution).toContain('© OpenStreetMap contributors');
  });

  it('chọn theo cấu hình: maptiler khi có key, osm chỉ ở dev, production thiếu key thì không có tile', () => {
    vi.spyOn(console, 'warn').mockImplementation(() => {});
    vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(basemapFor({ basemap: 'maptiler', maptilerKey: 'k' }, false).id).toBe('maptiler-streets-v2');
    expect(basemapFor({ basemap: 'osm', maptilerKey: '' }, true)).toEqual(osm());
    expect(basemapFor({ basemap: 'maptiler', maptilerKey: '' }, true)).toEqual(osm());
    expect(basemapFor({ basemap: 'osm', maptilerKey: '' }, false)).toEqual(blankBasemap());
    expect(basemapFor({ basemap: 'maptiler', maptilerKey: '' }, false).tileUrl).toBe('');
  });
});
