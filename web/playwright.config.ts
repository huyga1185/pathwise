import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end run against the real backend (W07, docs/client/ARCHITECTURE.md §10): the production build served by
 * `vite preview`, its /api proxied to the E2E compose stack (compose.e2e.yaml, ports in e2e/e2e.env).
 */
const API = process.env.PATHWISE_API_PROXY ?? 'http://localhost:8091';

export default defineConfig({
  testDir: './e2e',
  timeout: 240_000,
  expect: { timeout: 20_000 },
  fullyParallel: false,
  retries: 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://localhost:4173/app/',
    locale: 'vi-VN',
    timezoneId: 'Asia/Ho_Chi_Minh',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'pnpm build && pnpm preview --port 4173 --strictPort',
    url: 'http://localhost:4173/app/',
    reuseExistingServer: !process.env.CI,
    timeout: 180_000,
    // No MapTiler key: the build shows maps without tiles, so the run downloads no tiles.
    env: { PATHWISE_API_PROXY: API, VITE_BASEMAP: 'maptiler', VITE_MAPTILER_KEY: '' },
  },
});
