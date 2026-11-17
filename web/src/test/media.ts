import { vi } from 'vitest';

/**
 * Makes the app's 960px media query match, as on a computer: sidebar, tables. Tests otherwise run at phone width
 * (setup.ts). Restore with vi.restoreAllMocks().
 */
export function wideWindow() {
  vi.spyOn(window, 'matchMedia').mockImplementation(
    (query: string) =>
      ({
        matches: query.includes('min-width: 960px'),
        media: query,
        addEventListener: () => {},
        removeEventListener: () => {},
      }) as unknown as MediaQueryList,
  );
}
