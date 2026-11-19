import { useEffect, useState, type KeyboardEvent, type ReactNode } from 'react';
import { Bar, BarChart, CartesianGrid, Cell, ResponsiveContainer, XAxis, YAxis } from 'recharts';

/** One bar series: a key of the data, its label, and the design token of its color (`route`, `success`…). */
export interface ChartSeries {
  key: string;
  label: string;
  token: string;
}

export interface DailyBarChartProps {
  /**
   * One point per day, in order: `label` on the axis (e.g. "14/11"), `spoken` the day for assistive technologies
   * ("Thứ Hai, 1 tháng 12, 2026"; falls back to `label`), a number per series key.
   */
  data: Array<{ label: string; spoken?: string } & Record<string, number | string | undefined>>;
  series: ChartSeries[];
  /** Series drawn on top of each other (stop results) instead of side by side. */
  stacked?: boolean;
  /** What the chart shows, its name for assistive technologies. */
  ariaLabel: string;
  height?: number;
  /** The line over the chart: the whole period (null) or the selected day (its index); read out when it changes. */
  summary?: (index: number | null) => ReactNode;
}

/**
 * Bar chart by day (W11, P-59; G6, OWNER_UI_REDESIGN §11): the reports page and the ADMIN overview. The chart library
 * stays in this folder; screens pass plain data, labels and token names. HIG Charts: a legend with words (colour is
 * never the only cue, WCAG 1.4.1), separators between stacked marks, a summary line; a day is selected by pointing,
 * tapping or the arrow keys anywhere on the plot (the whole plot is the target), its bars stand out and the summary
 * says its numbers — nothing needs a hover. Assistive technologies get one entry per day with its full date and
 * values; the drawn axes are hidden from them. Colors are read from the theme's tokens.
 */
export function DailyBarChart({ data, series, stacked = false, ariaLabel, height = 220, summary }: DailyBarChartProps) {
  const colors = useTokenColors([...series.map((item) => item.token), 'ink-muted', 'line', 'surface-raised']);
  const [selected, setSelected] = useState<number | null>(null);
  const describe = (index: number) => {
    const point = data[index];
    if (!point) return '';
    const values = series.map((item) => `${Number(point[item.key] ?? 0)} ${item.label}`).join(', ');
    return `${point.spoken ?? point.label}: ${values}`;
  };

  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const last = data.length - 1;
    const now = selected;
    const next =
      event.key === 'ArrowRight'
        ? now === null
          ? 0
          : Math.min(last, now + 1)
        : event.key === 'ArrowLeft'
          ? now === null
            ? last
            : Math.max(0, now - 1)
          : event.key === 'Home'
            ? 0
            : event.key === 'End'
              ? last
              : event.key === 'Escape'
                ? null
                : undefined;
    if (next === undefined) return;
    event.preventDefault();
    setSelected(next);
  };

  return (
    <figure className="pw-chart" aria-label={ariaLabel} role="group">
      {summary ? (
        <p className="pw-chart-summary" aria-live="polite">
          {summary(selected)}
        </p>
      ) : null}
      {/* One tab stop: arrows choose a day; the drawing itself is not read (the list below is). */}
      <div
        className="pw-chart-plot"
        tabIndex={0}
        // Choosing a day with the arrow keys is a slider; its value is the day and its numbers.
        role="slider"
        aria-label={ariaLabel}
        aria-valuemin={1}
        aria-valuemax={data.length}
        aria-valuenow={selected === null ? undefined : selected + 1}
        aria-valuetext={selected === null ? 'Chưa chọn ngày; dùng phím mũi tên để chọn' : describe(selected)}
        onKeyDown={onKeyDown}
        onBlur={() => setSelected(null)}
        style={{ height }}
      >
        <div aria-hidden="true" style={{ height: '100%' }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart
              data={data}
              margin={{ top: 8, right: 8, bottom: 0, left: -16 }}
              accessibilityLayer={false}
              onMouseMove={(state) => {
                const index = Number(state?.activeTooltipIndex);
                if (Number.isInteger(index)) setSelected(index);
              }}
              onClick={(state) => {
                const index = Number(state?.activeTooltipIndex);
                if (Number.isInteger(index)) setSelected(index);
              }}
              onMouseLeave={() => setSelected(null)}
            >
              <CartesianGrid vertical={false} stroke={colors['line']} />
              <XAxis
                dataKey="label"
                tick={{ fill: colors['ink-muted'], fontSize: 12 }}
                tickLine={false}
                axisLine={false}
                minTickGap={16}
              />
              <YAxis
                allowDecimals={false}
                tick={{ fill: colors['ink-muted'], fontSize: 12 }}
                tickLine={false}
                axisLine={false}
                width={48}
              />
              {series.map((item) => (
                <Bar
                  key={item.key}
                  dataKey={item.key}
                  name={item.label}
                  fill={colors[item.token]}
                  stackId={stacked ? 'all' : undefined}
                  radius={stacked ? 0 : [2, 2, 0, 0]}
                  // Separators between stacked marks (HIG Charts): an edge in the surface's colour.
                  stroke={stacked ? colors['surface-raised'] : undefined}
                  strokeWidth={stacked ? 1 : 0}
                  isAnimationActive={false}
                >
                  {data.map((_, index) => (
                    <Cell key={index} fillOpacity={selected === null || selected === index ? 1 : 0.35} />
                  ))}
                </Bar>
              ))}
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
      <ul className="pw-chart-legend" aria-hidden="true">
        {series.map((item) => (
          <li key={item.key}>
            <span className="pw-chart-swatch" style={{ background: colors[item.token] }} />
            {item.label}
          </li>
        ))}
      </ul>
      {/* Each day with its full date and values, for assistive technologies (HIG Charts: an accessibility label for
          each important element). */}
      <ul className="pw-visually-hidden">
        {data.map((_, index) => (
          <li key={index}>{describe(index)}</li>
        ))}
      </ul>
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
