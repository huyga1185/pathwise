// Lint rules carry the hard rules of docs/client/ARCHITECTURE.md §2 and §12 (layers, ports, security).
import js from '@eslint/js';
import { createTypeScriptImportResolver } from 'eslint-import-resolver-typescript';
import { importX } from 'eslint-plugin-import-x';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

// Layers, top to bottom: app → features → shared → data → platform. A layer never imports one above it.
const layerZones = [
  {
    target: './src/shared',
    from: ['./src/app', './src/features'],
    message: 'shared/ must not depend on app/ or features/ (ARCHITECTURE §2).',
  },
  {
    target: './src/data',
    from: ['./src/app', './src/features', './src/shared'],
    message: 'data/ must not depend on app/, features/ or shared/ (ARCHITECTURE §2).',
  },
  {
    target: './src/platform',
    from: ['./src/app', './src/features', './src/shared', './src/data'],
    message: 'platform/ is the bottom layer (ARCHITECTURE §2).',
  },
  { target: './src/features', from: ['./src/app'], message: 'features/ must not depend on app/ (ARCHITECTURE §2).' },
];

export default tseslint.config(
  { ignores: ['dist', 'coverage', 'src/data/schema.d.ts'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['**/*.{ts,tsx,js}'],
    languageOptions: { globals: { ...globals.browser, ...globals.node } },
    plugins: { 'react-hooks': reactHooks, 'import-x': importX },
    settings: {
      'import-x/resolver-next': [createTypeScriptImportResolver({ project: './tsconfig.json' })],
    },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'no-console': ['error', { allow: ['warn', 'error'] }],
      'no-restricted-syntax': [
        'error',
        {
          selector: "JSXAttribute[name.name='dangerouslySetInnerHTML']",
          message: 'Never render HTML from data (ARCHITECTURE §12).',
        },
      ],
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/features/*/*'],
              message: "Import another feature only through its index ('@/features/<name>') (ARCHITECTURE §2 rule 2).",
            },
            {
              group: ['leaflet', 'leaflet/*', 'react-leaflet', 'react-leaflet/*'],
              message: 'The map library is only used inside src/platform/map (P-45).',
            },
            {
              group: ['@dnd-kit/*'],
              message: 'The drag-and-drop library is only used inside src/platform/dnd (P-58).',
            },
            {
              group: ['recharts', 'recharts/*'],
              message: 'The chart library is only used inside src/platform/charts (P-59, W11).',
            },
          ],
        },
      ],
      'import-x/no-cycle': 'error',
      'import-x/no-restricted-paths': ['error', { zones: layerZones }],
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    },
  },
  {
    // The E2E map stub is a plain Node script run in a container (W07).
    files: ['e2e/stub/**/*.mjs'],
    languageOptions: { globals: { ...globals.node } },
    rules: { 'no-console': 'off' },
  },
  {
    // The map, drag-and-drop and chart platforms are the one place allowed to use their library.
    files: ['src/platform/map/**', 'src/platform/dnd/**', 'src/platform/charts/**'],
    rules: {
      'no-restricted-imports': [
        'error',
        { patterns: [{ group: ['@/features/*/*'], message: 'Import features only through their index.' }] },
      ],
    },
  },
);
