import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

/**
 * A response body from docs/ux/samples (real backend output, MvpScenarioIntegrationTest), as test data
 * (docs/client/ARCHITECTURE.md §10). Returns a fresh copy each time.
 */
export function sample<T = { data: unknown }>(name: string): T {
  // Vitest runs in web/; the samples are in the repository's docs.
  const file = resolve(process.cwd(), '..', 'docs', 'ux', 'samples', `${name}.json`);
  return JSON.parse(readFileSync(file, 'utf8')) as T;
}
