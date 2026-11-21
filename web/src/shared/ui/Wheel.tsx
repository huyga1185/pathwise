import { useEffect, useRef, type KeyboardEvent } from 'react';
import { reducedMotion } from '@/shared/hooks/useExit';
import { cx } from './cx';

const ITEM = 36;

/**
 * One wheel of a picker (HIG Pickers: "Wheels — A set of scrolling wheels"; the month and year wheels an iOS calendar
 * shows from its "Month Year ›" button, OWNER_UI_REDESIGN §8.12): a column that scrolls and snaps, the value in the
 * middle band. Scroll, swipe or the mouse wheel turns it; a click on a value turns it there; for the keyboard and
 * assistive technologies it is a spin button (arrows, Page Up/Down, Home/End).
 */
export function Wheel<T extends number | string>({
  label,
  items,
  value,
  onChange,
}: {
  label: string;
  items: readonly { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
}) {
  const scroller = useRef<HTMLDivElement>(null);
  const index = Math.max(
    0,
    items.findIndex((item) => item.value === value),
  );
  const settle = useRef<number | undefined>(undefined);
  const latest = useRef({ items, index, onChange });
  useEffect(() => {
    latest.current = { items, index, onChange };
  });

  // The value set from outside (or on opening): stand on it without animating.
  useEffect(() => {
    const element = scroller.current;
    if (element && Math.round(element.scrollTop / ITEM) !== index) element.scrollTop = index * ITEM;
  }, [index]);

  // Where the wheel comes to rest is the value.
  useEffect(() => {
    const element = scroller.current;
    if (!element) return undefined;
    const commit = () => {
      const { items: all, index: current, onChange: change } = latest.current;
      const next = Math.min(all.length - 1, Math.max(0, Math.round(element.scrollTop / ITEM)));
      if (next !== current) change(all[next]!.value);
    };
    const onScroll = () => {
      window.clearTimeout(settle.current);
      settle.current = window.setTimeout(commit, 120);
    };
    element.addEventListener('scroll', onScroll, { passive: true });
    return () => {
      element.removeEventListener('scroll', onScroll);
      window.clearTimeout(settle.current);
    };
  }, []);

  const turnTo = (next: number) => {
    const target = Math.min(items.length - 1, Math.max(0, next));
    scroller.current?.scrollTo?.({ top: target * ITEM, behavior: reducedMotion() ? 'auto' : 'smooth' });
    if (target !== index) onChange(items[target]!.value);
  };
  const onKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
    const step = { ArrowUp: -1, ArrowDown: 1, PageUp: -5, PageDown: 5 }[event.key];
    const next =
      step !== undefined ? index + step : event.key === 'Home' ? 0 : event.key === 'End' ? items.length - 1 : null;
    if (next === null) return;
    event.preventDefault();
    turnTo(next);
  };

  return (
    <div
      className="pw-wheel"
      role="spinbutton"
      tabIndex={0}
      aria-label={label}
      aria-valuemin={0}
      aria-valuemax={items.length - 1}
      aria-valuenow={index}
      aria-valuetext={items[index]?.label}
      onKeyDown={onKeyDown}
    >
      <div ref={scroller} className="pw-wheel-scroller" aria-hidden="true">
        {items.map((item, position) => (
          <div
            key={String(item.value)}
            className={cx('pw-wheel-item', position === index && 'is-current')}
            onClick={() => turnTo(position)}
          >
            {item.label}
          </div>
        ))}
      </div>
    </div>
  );
}
