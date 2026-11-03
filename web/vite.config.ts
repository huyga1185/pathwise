/// <reference types="vitest/config" />
import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// P-47: the SPA lives under /app/ on the same origin as the API (/api/). In dev, Vite proxies /api to the backend.
export default defineConfig({
  base: '/app/',
  plugins: [react()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: false } },
    // Logos and favicons come from brand/logo (docs/client/ARCHITECTURE.md §8).
    fs: { allow: ['..'] },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
    // Node's Request needs an absolute URL; the browser build keeps the same-origin default (P-47).
    env: { VITE_API_ORIGIN: 'http://localhost' },
  },
});
