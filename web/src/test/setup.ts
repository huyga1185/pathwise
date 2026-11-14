import '@testing-library/jest-dom/vitest';
import { cleanup, configure } from '@testing-library/react';
import { afterAll, afterEach, beforeAll } from 'vitest';
import { server } from './server';

const hasDom = typeof window !== 'undefined';

// The whole suite runs in parallel workers (maps included): a first load can take over the 1 s default.
configure({ asyncUtilTimeout: 3000 });

// jsdom has no matchMedia; the theme reads prefers-color-scheme.
if (hasDom && !window.matchMedia) {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: (query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addEventListener: () => {},
      removeEventListener: () => {},
      addListener: () => {},
      removeListener: () => {},
      dispatchEvent: () => false,
    }),
  });
}

// jsdom has no SVG geometry, so Leaflet would draw route lines on a canvas; real browsers use SVG.
if (hasDom && typeof SVGSVGElement !== 'undefined' && !('createSVGRect' in SVGSVGElement.prototype)) {
  Object.defineProperty(SVGSVGElement.prototype, 'createSVGRect', {
    value: () => ({ x: 0, y: 0, width: 0, height: 0 }),
  });
}

// jsdom has no ResizeObserver; the chart platform (Recharts, W11) sizes itself with it.
if (hasDom && !('ResizeObserver' in window)) {
  class NoResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
  Object.defineProperty(window, 'ResizeObserver', { writable: true, value: NoResizeObserver });
}

beforeAll(() => server.listen({ onUnhandledFrame: 'error' }));
afterEach(() => {
  if (!hasDom) return;
  cleanup();
  server.resetHandlers();
  document.documentElement.removeAttribute('data-theme');
});
afterAll(() => server.close());
