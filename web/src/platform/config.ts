/**
 * AppConfig port (docs/client/ARCHITECTURE.md §2.1): every environment setting the app reads.
 *
 * Two sources (W08, P-56): the runtime config of the deployment, `window.__PATHWISE_CONFIG__` from `/app/config.js`
 * (the web container writes it from its environment when it starts), then the Vite env variables of the build. A
 * non-empty runtime value wins, so one built image serves any domain or map key.
 */
export interface AppConfig {
  /** Origin of the API; empty means the same origin as the app (P-47). Paths already carry /api/v1. */
  apiOrigin: string;
  basemap: string;
  maptilerKey: string;
}

/** Shape of `/app/config.js`; every field is optional and only non-empty strings count. */
export interface RuntimeConfig {
  apiOrigin?: unknown;
  basemap?: unknown;
  maptilerKey?: unknown;
}

declare global {
  interface Window {
    __PATHWISE_CONFIG__?: RuntimeConfig;
  }
}

export function readConfig(
  env: Record<string, string | boolean | undefined> = import.meta.env,
  runtime: RuntimeConfig | undefined = typeof window === 'undefined' ? undefined : window.__PATHWISE_CONFIG__,
): AppConfig {
  const fromEnv = (key: string) => (typeof env[key] === 'string' ? (env[key] as string).trim() : '');
  const pick = (value: unknown, key: string) => (typeof value === 'string' ? value.trim() : '') || fromEnv(key);
  return {
    apiOrigin: pick(runtime?.apiOrigin, 'VITE_API_ORIGIN').replace(/\/+$/, ''),
    basemap: pick(runtime?.basemap, 'VITE_BASEMAP') || 'maptiler',
    maptilerKey: pick(runtime?.maptilerKey, 'VITE_MAPTILER_KEY'),
  };
}
