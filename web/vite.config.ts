/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// P-47: the SPA lives under /app/ on the same origin as the API (/api/). In dev, Vite proxies /api to the backend.
export default defineConfig({
  base: '/app/',
  plugins: [react()],
  // Favicons and logos come straight from brand/logo (docs/client/ARCHITECTURE.md §8): served under /app/ in dev, copied into dist.
  publicDir: '../brand/logo',
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    // `vite preview` uses the same proxy; E2E points it at its own stack (PATHWISE_API_PROXY, web/e2e).
    proxy: { '/api': { target: process.env.PATHWISE_API_PROXY ?? 'http://localhost:8080', changeOrigin: false } },
  },
  test: {
    environment: 'jsdom',
    // Playwright owns e2e/ (W07).
    include: ['src/**/*.test.{ts,tsx}'],
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
    // Node's Request needs an absolute URL; the browser build keeps the same-origin default (P-47).
    env: { VITE_API_ORIGIN: 'http://localhost' },
  },
});
