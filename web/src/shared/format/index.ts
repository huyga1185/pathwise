/** Display formats of docs/client/ARCHITECTURE.md §6 and UX_BRIEF §2 (vi-VN, the plan's or store's timezone). */

/** "+84905123456" → "0905 123 456"; anything else unchanged. */
export function formatPhone(e164: string | null | undefined): string {
  if (!e164) return '';
  const match = /^\+84(\d{9})$/.exec(e164);
  if (!match) return e164;
  const digits = `0${match[1]}`;
  return `${digits.slice(0, 4)} ${digits.slice(4, 7)} ${digits.slice(7)}`;
}

/** Metres → "12,4 km" (one decimal, decimal comma). */
export function formatDistance(meters: number | null | undefined): string {
  if (meters === null || meters === undefined) return '—';
  return `${(meters / 1000).toLocaleString('vi-VN', { minimumFractionDigits: 1, maximumFractionDigits: 1 })} km`;
}

/** Seconds → "2 giờ 35 phút", "45 phút". Rounded to the minute. */
export function formatDuration(seconds: number | null | undefined): string {
  if (seconds === null || seconds === undefined) return '—';
  const minutes = Math.round(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  if (hours === 0) return `${rest} phút`;
  return `${hours} giờ ${String(rest).padStart(2, '0')} phút`;
}

/** An instant (ISO UTC) as "HH:mm" in the given timezone, 24-hour. */
export function formatTime(instant: string | null | undefined, timeZone: string): string {
  if (!instant) return '—';
  return new Intl.DateTimeFormat('vi-VN', { hour: '2-digit', minute: '2-digit', hourCycle: 'h23', timeZone }).format(
    new Date(instant),
  );
}

/** A local date "2026-12-01" as "Thứ Ba, 01/12/2026". */
export function formatDateLong(localDate: string): string {
  const [year, month, day] = localDate.split('-').map(Number);
  const date = new Date(Date.UTC(year ?? 1970, (month ?? 1) - 1, day ?? 1));
  const weekday = new Intl.DateTimeFormat('vi-VN', { weekday: 'long', timeZone: 'UTC' }).format(date);
  const dmy = new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    timeZone: 'UTC',
  }).format(date);
  return `${weekday.charAt(0).toLocaleUpperCase('vi-VN')}${weekday.slice(1)}, ${dmy}`;
}

/** "03/11 16:20": a moment (ISO UTC) as day/month and 24-hour time in `timeZone`. */
export function formatDayTime(instant: string | null | undefined, timeZone: string): string {
  if (!instant) return '';
  const parts = new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hourCycle: 'h23',
    timeZone,
  }).formatToParts(new Date(instant));
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
  return `${get('day')}/${get('month')} ${get('hour')}:${get('minute')}`;
}

/** "10/11/2026": the date of a moment (ISO UTC) in `timeZone` (the device's when omitted). */
export function formatDate(instant: string | null | undefined, timeZone?: string): string {
  if (!instant) return '';
  return new Intl.DateTimeFormat('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', timeZone }).format(
    new Date(instant),
  );
}

/** "0:42", "1:05": seconds as minutes and seconds, for a short wait shown on a button. */
export function formatClock(seconds: number): string {
  const s = Math.max(0, Math.ceil(seconds));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;
}

/** Today as "YYYY-MM-DD" in `timeZone` (a plan's date is the store's local date). */
export function todayIn(timeZone: string, now: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-CA', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    timeZone,
  }).formatToParts(now);
  const get = (type: string) => parts.find((part) => part.type === type)?.value ?? '';
  return `${get('year')}-${get('month')}-${get('day')}`;
}
