// @vitest-environment node
import { ESLint } from 'eslint';
import { describe, expect, it } from 'vitest';

/** The map library stays inside src/platform/map (P-45, W04 acceptance): the lint rule is what enforces it. */
describe('ESLint: thư viện bản đồ', () => {
  const lint = async (filePath: string, code: string) => {
    const [result] = await new ESLint().lintText(code, { filePath });
    return (result?.messages ?? []).map((message) => message.ruleId);
  };

  it('chặn import leaflet/react-leaflet ngoài platform/map', async () => {
    expect(await lint('src/features/store/Example.tsx', "import 'leaflet';\n")).toContain('no-restricted-imports');
    expect(await lint('src/shared/ui/Example.tsx', "export { MapContainer } from 'react-leaflet';\n")).toContain(
      'no-restricted-imports',
    );
  });

  it('cho phép trong platform/map', async () => {
    expect(await lint('src/platform/map/Example.ts', "import 'leaflet';\n")).not.toContain('no-restricted-imports');
  });
});

/** The chart library stays inside src/platform/charts (P-59, W11). */
describe('ESLint: thư viện biểu đồ', () => {
  const lint = async (filePath: string, code: string) => {
    const [result] = await new ESLint().lintText(code, { filePath });
    return (result?.messages ?? []).map((message) => message.ruleId);
  };

  it('chặn import recharts ngoài platform/charts, cho phép trong đó', async () => {
    expect(await lint('src/features/admin/Example.tsx', "export { BarChart } from 'recharts';\n")).toContain(
      'no-restricted-imports',
    );
    expect(await lint('src/platform/charts/Example.tsx', "export { BarChart } from 'recharts';\n")).not.toContain(
      'no-restricted-imports',
    );
  });
});
