import { useCallback, useEffect, useState } from 'react';

/**
 * Seconds left of a lock that ends at a moment in time (429 `Retry-After`, the 60 s before a new OTP may be sent).
 * `start(seconds)` begins or restarts it; `remaining` is 0 when unlocked.
 */
export function useCountdown(): { remaining: number; start(seconds: number): void } {
  const [until, setUntil] = useState<number | null>(null);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (until === null) return undefined;
    const timer = window.setInterval(() => {
      const current = Date.now();
      setNow(current);
      if (current >= until) setUntil(null);
    }, 1000);
    return () => window.clearInterval(timer);
  }, [until]);

  const start = useCallback((seconds: number) => {
    const current = Date.now();
    setNow(current);
    setUntil(seconds > 0 ? current + seconds * 1000 : null);
  }, []);

  const remaining = until === null ? 0 : Math.max(0, Math.ceil((until - now) / 1000));
  return { remaining, start };
}
