/** Display helpers of the ADMIN area (W11). */

/** 0.9647 → "96,5%"; null (nothing to divide) → "—" (UX_BRIEF §5.5). */
export function formatRate(rate: number | null | undefined): string {
  if (rate == null) return '—';
  return `${(Math.round(rate * 1000) / 10).toLocaleString('vi-VN')}%`;
}

/** 52428800 → "50,0 MB". */
export function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024).toLocaleString('vi-VN')} KB`;
  const mb = bytes / (1024 * 1024);
  if (mb < 1024) return `${mb.toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} MB`;
  return `${(mb / 1024).toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} GB`;
}

/** "2026-11-14" → "14/11". */
export function shortDate(day: string): string {
  const [, month, date] = day.split('-');
  return `${date}/${month}`;
}

/** Positive whole number typed in a filter, or undefined. */
export function idOrUndefined(text: string | null): number | undefined {
  if (!text) return undefined;
  const value = Number(text);
  return Number.isInteger(value) && value > 0 ? value : undefined;
}
