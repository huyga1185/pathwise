/**
 * AppConfig port (docs/client/ARCHITECTURE.md §2.1): every environment setting the app reads, from Vite env variables.
 */
export interface AppConfig {
  /** Origin of the API; empty means the same origin as the app (P-47). Paths already carry /api/v1. */
  apiOrigin: string;
  basemap: string;
  maptilerKey: string;
}

export function readConfig(env: Record<string, string | boolean | undefined> = import.meta.env): AppConfig {
  const text = (key: string) => (typeof env[key] === 'string' ? (env[key] as string).trim() : '');
  return {
    apiOrigin: text('VITE_API_ORIGIN').replace(/\/+$/, ''),
    basemap: text('VITE_BASEMAP') || 'maptiler',
    maptilerKey: text('VITE_MAPTILER_KEY'),
  };
}
