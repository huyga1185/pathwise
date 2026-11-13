/// <reference types="vitest/config" />
import type { IncomingMessage, ServerResponse } from 'node:http';
import { fileURLToPath, URL } from 'node:url';
import react from '@vitejs/plugin-react';
import { defineConfig, type Plugin } from 'vite';

/**
 * Runtime config (W08, P-56): /app/config.js sets window.__PATHWISE_CONFIG__ before the app starts. The build ships
 * an empty one, which keeps the values baked in at build time; the web container rewrites it from its environment.
 * Dev and preview serve the same empty file.
 */
function runtimeConfig(): Plugin {
  const source = 'window.__PATHWISE_CONFIG__ = window.__PATHWISE_CONFIG__ || {};\n';
  const serve = (_req: IncomingMessage, res: ServerResponse) => {
    res.setHeader('Content-Type', 'text/javascript; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store');
    res.end(source);
  };
  return {
    name: 'pathwise-runtime-config',
    configureServer: (server) => void server.middlewares.use('/app/config.js', serve),
    configurePreviewServer: (server) => void server.middlewares.use('/app/config.js', serve),
    generateBundle() {
      this.emitFile({ type: 'asset', fileName: 'config.js', source });
    },
  };
}

export default defineConfig({
  base: '/app/',
  plugins: [react(), runtimeConfig()],
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
