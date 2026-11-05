import { createContext, useContext, type ReactNode } from 'react';
import { blankBasemap, type Basemap } from './basemap';

const BasemapContext = createContext<Basemap>(blankBasemap());

/** Gives the maps the basemap chosen in app/ (composition root). */
export function BasemapProvider({ basemap, children }: { basemap: Basemap; children: ReactNode }) {
  return <BasemapContext.Provider value={basemap}>{children}</BasemapContext.Provider>;
}

export function useBasemap(): Basemap {
  return useContext(BasemapContext);
}
