import { useEffect, useLayoutEffect, useRef, useState } from 'react';

/** How long a removed row fades out before it goes (plans.css `.is-leaving`). */
export const LEAVE_MS = 200;

/**
 * Rows coming and going in a list of the plan form (OWNER_UI_REDESIGN §9.6, owner's answer 2026-11-18), as a standard
 * table inserts and deletes rows: a row added after the list first showed fades in with a tint that dies away
 * (`is-new`), a removed row fades out (`is-leaving`) and is then taken out by `done`.
 *
 * `hold`: a row being filled in a sheet that covers the list (an added row on a phone): it arrives once the sheet
 * closes, where it can be seen, and not at all if the sheet is cancelled.
 */
export function useRowMotion(keys: string[], hold: string | null = null) {
  const known = useRef<Set<string> | null>(null);
  const [fresh, setFresh] = useState<ReadonlySet<string>>(() => new Set());
  const [leaving, setLeaving] = useState<ReadonlySet<string>>(() => new Set());
  const timers = useRef<number[]>([]);

  // Before paint: a new row never shows one frame without its arrival.
  useLayoutEffect(() => {
    if (known.current === null) {
      known.current = new Set(keys);
      return;
    }
    const seen = known.current;
    const added = keys.filter((key) => !seen.has(key) && key !== hold);
    if (added.length === 0) return;
    added.forEach((key) => seen.add(key));
    setFresh(new Set(added));
  }, [keys, hold]);

  useEffect(
    () => () => {
      timers.current.forEach((timer) => window.clearTimeout(timer));
    },
    [],
  );

  const leave = (key: string, done: () => void) => {
    if (leaving.has(key)) return;
    setLeaving((current) => new Set(current).add(key));
    timers.current.push(
      window.setTimeout(() => {
        setLeaving((current) => {
          const next = new Set(current);
          next.delete(key);
          return next;
        });
        done();
      }, LEAVE_MS),
    );
  };

  const rowClass = (key: string) => (leaving.has(key) ? 'is-leaving' : fresh.has(key) ? 'is-new' : undefined);
  return { rowClass, leave, isLeaving: (key: string) => leaving.has(key) };
}
