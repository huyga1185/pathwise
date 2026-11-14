import { useEffect, useState } from 'react';
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';

/** One bar series: a key of the data, its label, and the design token of its color (`route`, `success`…). */
export interface ChartSeries {
  key: string;
  label: string;
  token: string;
}

export interface DailyBarChartProps {
  /** One point per day, in order: `label` on the axis (e.g. "14/11"), a number per series key. */
  data: Array<{ label: string } & Record<string, number | string>>;
  series: ChartSeries[];
  /** Series drawn on top of each other (stop results) instead of side by side. */
  stacked?: boolean;
  /** What the chart shows, for screen readers; the numbers are also in the page as text. */
  ariaLabel: string;
  height?: number;
}

/**
 * Bar chart by day (W11, P-59): the one chart of the ADMIN overview. The chart library stays in this folder; screens
 * pass plain data, labels and token names, so changing the library touches nothing else. Colors are read from the
 * tokens of the current theme (`data-theme` on <html>) and read again when the theme changes.
 */
export function DailyBarChart({ data, series, stacked = false, ariaLabel, height = 220 }: DailyBarChartProps) {
  const colors = useTokenColors(series.map((item) => item.token));
  const axis = useTokenColors(['ink-muted', 'line']);
  return (
    <figure className="pw-chart" role="img" aria-label={ariaLabel} style={{ margin: 0, height }}>
      <ResponsiveContainer width="100%" height="100%">
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: -16 }}>
          <CartesianGrid vertical={false} stroke={axis['line']} />
          <XAxis
            dataKey="label"
            tick={{ fill: axis['ink-muted'], fontSize: 12 }}
            tickLine={false}
            axisLine={false}
            minTickGap={16}
          />
          <YAxis
            allowDecimals={false}
            tick={{ fill: axis['ink-muted'], fontSize: 12 }}
            tickLine={false}
            axisLine={false}
            width={48}
          />
          <Tooltip cursor={{ fill: axis['line'], fillOpacity: 0.3 }} />
          {series.map((item) => (
            <Bar
              key={item.key}
              dataKey={item.key}
              name={item.label}
              fill={colors[item.token]}
              stackId={stacked ? 'all' : undefined}
              radius={stacked ? 0 : [2, 2, 0, 0]}
              isAnimationActive={false}
            />
          ))}
        </BarChart>
      </ResponsiveContainer>
    </figure>
  );
}

/** Token name → its color now; SVG attributes cannot read CSS variables themselves. */
function useTokenColors(tokens: string[]): Record<string, string> {
  const key = tokens.join(',');
  const [theme, setTheme] = useState(() => document.documentElement.dataset.theme ?? '');
  useEffect(() => {
    const observer = new MutationObserver(() => setTheme(document.documentElement.dataset.theme ?? ''));
    observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] });
    return () => observer.disconnect();
  }, []);
  const style = getComputedStyle(document.documentElement);
  const colors: Record<string, string> = {};
  for (const token of key.split(',')) colors[token] = style.getPropertyValue(`--${token}`).trim() || 'currentColor';
  void theme;
  return colors;
}
