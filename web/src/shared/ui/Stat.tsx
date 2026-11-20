import { Children, type CSSProperties, type ReactNode } from 'react';

/**
 * Numbers in one group (G6.3, reused by the ADMIN pages, §12): a filled block, radius 16, no outline, the numbers
 * side by side with hairlines between (HIG Layout: group related items; WWDC25 356: hierarchy through grouping, not
 * outlines). As many columns as numbers; narrow (< 640px of room), two per row, an odd last one across.
 */
export function StatGroup({ label, children }: { label: string; children: ReactNode }) {
  const style = { '--stat-cols': Children.count(children) } as CSSProperties;
  return (
    <section className="pw-statgroup" aria-label={label}>
      <div className="pw-statgroup-grid" style={style}>
        {children}
      </div>
    </section>
  );
}

/** One number of a StatGroup: its label, the number, a line about it. */
export function Stat({ label, value, sub }: { label: string; value: string | number; sub?: string }) {
  return (
    <div className="pw-statgroup-item">
      <span className="pw-statgroup-item-label">{label}</span>
      <span className="pw-statgroup-item-value pw-tabular">{value}</span>
      {sub ? <span className="pw-muted pw-statgroup-item-sub">{sub}</span> : null}
    </div>
  );
}
