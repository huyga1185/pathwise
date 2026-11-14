/** Display helpers of the ADMIN area (W11). */

/** 52428800 → "50,0 MB". */
export function formatBytes(bytes: number): string {
  if (bytes < 1024 * 1024) return `${Math.round(bytes / 1024).toLocaleString('vi-VN')} KB`;
  const mb = bytes / (1024 * 1024);
  if (mb < 1024) return `${mb.toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} MB`;
  return `${(mb / 1024).toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} GB`;
}

/** Positive whole number typed in a filter, or undefined. */
export function idOrUndefined(text: string | null): number | undefined {
  if (!text) return undefined;
  const value = Number(text);
  return Number.isInteger(value) && value > 0 ? value : undefined;
}
