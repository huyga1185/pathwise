import { useLayoutEffect, useState, type RefObject } from 'react';

/**
 * Rows of the table that fit the window under the list's top with the pager below, nothing to scroll; measured again
 * when the window, the filters' row or the text size changes (rows grow with larger text, so fewer fit — HIG Layout).
 * At least 3 (a very short window scrolls), at most 100 (API_SPEC §1). `null` until measured; 20 where nothing can
 * be measured (tests). Also the height of a full page, which the list keeps on a short last page. The probe holds a
 * hidden table with one row as tall as a real row can be, and a pager (OWNER_UI_REDESIGN G2.9).
 */
export function useFitRows(
  enabled: boolean,
  result: RefObject<HTMLDivElement | null>,
  probe: RefObject<HTMLDivElement | null>,
  filters: RefObject<HTMLDivElement | null>,
  /** Rows where nothing can be measured (tests). */
  fallback: number,
): { rows: number; height: number } | null {
  const [fit, setFit] = useState<{ rows: number; height: number } | null>(null);
  useLayoutEffect(() => {
    if (!enabled) return undefined;
    const measure = () => {
      const list = result.current;
      const sample = probe.current;
      if (!list || !sample) return;
      const head = sample.querySelector('thead')?.getBoundingClientRect().height ?? 0;
      const row = sample.querySelector('tbody tr')?.getBoundingClientRect().height ?? 0;
      const pager = sample.querySelector('.pw-pager')?.getBoundingClientRect().height ?? 0;
      if (row <= 0) {
        setFit({ rows: fallback, height: 0 });
        return;
      }
      const main = list.closest('main');
      const bottom = main ? Number.parseFloat(getComputedStyle(main).paddingBottom) || 0 : 0;
      const top = list.getBoundingClientRect().top + window.scrollY;
      const gap = Number.parseFloat(getComputedStyle(list.parentElement ?? list).rowGap) || 0; // table ↔ pager
      const room = window.innerHeight - top - head - gap - pager - bottom - 2;
      // Whole pixels: a row measures 56.5 here and 57 in the table (borders), which adds up over a page.
      const rows = Math.max(3, Math.min(100, Math.floor(room / Math.ceil(row))));
      const height = Math.ceil(head + rows * Math.ceil(row) + 2);
      setFit((was) => (was && was.rows === rows && was.height === height ? was : { rows, height }));
    };
    measure();
    window.addEventListener('resize', measure);
    const observer = typeof ResizeObserver === 'undefined' ? null : new ResizeObserver(measure);
    if (probe.current) observer?.observe(probe.current);
    if (filters.current) observer?.observe(filters.current);
    return () => {
      window.removeEventListener('resize', measure);
      observer?.disconnect();
    };
  }, [enabled, result, probe, filters, fallback]);
  return enabled ? fit : null;
}
