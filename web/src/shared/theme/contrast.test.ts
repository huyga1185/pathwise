import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';

/**
 * W01 acceptance "no text under 4.5:1 in the dark theme" (both themes here). jsdom cannot resolve CSS variables or
 * paint, so axe's color-contrast rule cannot run; instead every text/background pair the components use is checked
 * on the token values themselves (brand/design-system/tokens.json).
 */
const tokens = JSON.parse(readFileSync(resolve(__dirname, '../../../../brand/design-system/tokens.json'), 'utf8')) as {
  color: { tokens: Array<{ name: string; value: Record<string, string> }> };
};
const colour = (name: string, theme: 'light' | 'dark') => {
  const token = tokens.color.tokens.find((t) => t.name === name);
  if (!token) throw new Error(`no token ${name}`);
  return token.value[theme] ?? token.value.light ?? '';
};
const luminance = (hex: string) => {
  const channels = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255);
  const [r, g, b] = channels.map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)) as [
    number,
    number,
    number,
  ];
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
};
const ratio = (a: string, b: string) => {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x) as [number, number];
  return (hi + 0.05) / (lo + 0.05);
};

const SURFACES = ['surface', 'surface-raised', 'surface-tint'];
const PAIRS: Array<[string, string]> = [
  ...SURFACES.flatMap((bg) => ['ink', 'ink-muted', 'route', 'danger'].map((fg) => [fg, bg] as [string, string])),
  // Pills, primary and result buttons: on-accent text on the accent colours.
  ...['route', 'info', 'signal', 'danger', 'success', 'ink-muted'].map((bg) => ['on-accent', bg] as [string, string]),
];

describe('tương phản token', () => {
  for (const theme of ['light', 'dark'] as const) {
    it.each(PAIRS)(`${theme}: chữ %s trên nền %s đạt 4,5:1`, (fg, bg) => {
      expect(ratio(colour(fg, theme), colour(bg, theme))).toBeGreaterThanOrEqual(4.5);
    });
  }
});
