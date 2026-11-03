/**
 * A readable name for a session's `userAgent` ("Chrome trên Linux", "App PathWise trên Android"). Display only: the
 * server keeps the raw header; an unknown agent shows `fallback`.
 */
export function deviceName(userAgent: string | null | undefined, fallback: string): string {
  if (!userAgent) return fallback;
  const ua = userAgent;
  const os = /iPhone|iPad|iPod/.test(ua)
    ? 'iOS'
    : /Android/.test(ua)
      ? 'Android'
      : /Windows/.test(ua)
        ? 'Windows'
        : /Mac OS X|Macintosh/.test(ua)
          ? 'macOS'
          : /CrOS/.test(ua)
            ? 'ChromeOS'
            : /Linux/.test(ua)
              ? 'Linux'
              : null;
  if (/^Dart\//.test(ua) || /PathWise/i.test(ua)) return os ? `App PathWise trên ${os}` : 'App PathWise';
  const browser = /Edg\//.test(ua)
    ? 'Edge'
    : /OPR\//.test(ua)
      ? 'Opera'
      : /Firefox\//.test(ua)
        ? 'Firefox'
        : /Chrome\//.test(ua)
          ? 'Chrome'
          : /Safari\//.test(ua)
            ? 'Safari'
            : null;
  if (browser && os) return `${browser} trên ${os}`;
  return browser ?? os ?? fallback;
}
