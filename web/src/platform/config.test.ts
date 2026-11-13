import { describe, expect, it } from 'vitest';
import { readConfig } from './config';

describe('readConfig', () => {
  const env = { VITE_API_ORIGIN: 'https://api.example.com/', VITE_BASEMAP: 'osm', VITE_MAPTILER_KEY: 'build-key' };

  it('reads the Vite env variables when there is no runtime config', () => {
    expect(readConfig(env, undefined)).toEqual({
      apiOrigin: 'https://api.example.com',
      basemap: 'osm',
      maptilerKey: 'build-key',
    });
  });

  it('prefers non-empty runtime values over the build', () => {
    expect(readConfig(env, { basemap: 'maptiler', maptilerKey: ' runtime-key ' })).toEqual({
      apiOrigin: 'https://api.example.com',
      basemap: 'maptiler',
      maptilerKey: 'runtime-key',
    });
  });

  it('ignores empty or non-string runtime values', () => {
    expect(readConfig(env, { apiOrigin: '', basemap: 42, maptilerKey: '   ' })).toEqual({
      apiOrigin: 'https://api.example.com',
      basemap: 'osm',
      maptilerKey: 'build-key',
    });
  });

  it('defaults to MapTiler and the same origin when nothing is set', () => {
    expect(readConfig({}, {})).toEqual({ apiOrigin: '', basemap: 'maptiler', maptilerKey: '' });
  });
});
