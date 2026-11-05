import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState, type ReactNode } from 'react';
import { ApiProvider } from '@/data/ApiContext';
import { createApi } from '@/data/http';
import { SessionProvider } from '@/features/auth';
import { readConfig } from '@/platform/config';
// Only the basemap choice here: the map components (and Leaflet) load with the screens that draw a map.
import { basemapFor, type Basemap } from '@/platform/map/basemap';
import { BasemapProvider } from '@/platform/map/BasemapContext';
import { MemoryTokenStore } from '@/platform/tokenStore';
import { ThemeProvider } from '@/shared/theme/ThemeProvider';

function createQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false } } });
}

/**
 * Composition root (docs/client/ARCHITECTURE.md §2.1): builds the ports' implementations from the configuration and
 * provides them. Tests pass their own `fetch` and query client.
 */
export function AppProviders({
  children,
  fetchImpl,
  queryClient,
  basemap,
}: {
  children: ReactNode;
  fetchImpl?: typeof fetch;
  queryClient?: QueryClient;
  /** Tests pass a blank one; otherwise chosen from VITE_BASEMAP / VITE_MAPTILER_KEY (P-45). */
  basemap?: Basemap;
}) {
  const [deps] = useState(() => {
    const config = readConfig();
    const tokenStore = new MemoryTokenStore();
    const sessionEvents = new EventTarget();
    const api = createApi({
      origin: config.apiOrigin,
      tokenStore,
      onSessionExpired: () => sessionEvents.dispatchEvent(new Event('expired')),
      fetch: fetchImpl ?? ((input, init) => globalThis.fetch(input, init)),
    });
    return {
      client: queryClient ?? createQueryClient(),
      api: { api, tokenStore, sessionEvents },
      basemap: basemap ?? basemapFor(config, import.meta.env.DEV),
    };
  });
  return (
    <ThemeProvider>
      <QueryClientProvider client={deps.client}>
        <ApiProvider value={deps.api}>
          <BasemapProvider basemap={deps.basemap}>
            <SessionProvider>{children}</SessionProvider>
          </BasemapProvider>
        </ApiProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}
