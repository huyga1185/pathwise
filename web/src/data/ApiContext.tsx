import { createContext, useContext, type ReactNode } from 'react';
import type { TokenStore } from '@/platform/tokenStore';
import type { Api } from './http';

interface ApiContextValue {
  api: Api;
  tokenStore: TokenStore;
  /** Fires 'expired' when a refresh fails while signed in. */
  sessionEvents: EventTarget;
}

const ApiContext = createContext<ApiContextValue | null>(null);

/** Gives screens the API built in app/ (the composition root, docs/client/ARCHITECTURE.md §2.1). */
export function ApiProvider({ value, children }: { value: ApiContextValue; children: ReactNode }) {
  return <ApiContext.Provider value={value}>{children}</ApiContext.Provider>;
}

export function useApiContext(): ApiContextValue {
  const value = useContext(ApiContext);
  if (!value) throw new Error('useApiContext must be used inside ApiProvider');
  return value;
}

export function useApi() {
  return useApiContext().api.client;
}
