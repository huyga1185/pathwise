import { useRef, type KeyboardEvent } from 'react';
import { cx } from './cx';

/**
 * A segmented control to switch between closely related views (HIG Segmented controls, iOS: "Consider a segmented
 * control to switch between closely related subviews"): equal segments in one capsule, the chosen one raised; text
 * only, nouns (HIG: "Prefer using either text or images", "Use nouns"). Tabs for assistive technologies; arrows move.
 */
export function SegmentedControl<T extends string>({
  label,
  value,
  options,
  onChange,
  idPrefix,
}: {
  label: string;
  value: T;
  options: readonly { value: T; label: string; panelId?: string }[];
  onChange: (value: T) => void;
  idPrefix: string;
}) {
  const refs = useRef<Record<string, HTMLButtonElement | null>>({});
  const onKey = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return;
    event.preventDefault();
    const index = options.findIndex((option) => option.value === value);
    const next = options[(index + (event.key === 'ArrowRight' ? 1 : -1) + options.length) % options.length];
    if (!next) return;
    onChange(next.value);
    refs.current[next.value]?.focus();
  };
  const index = Math.max(
    0,
    options.findIndex((option) => option.value === value),
  );
  return (
    <div className="pw-segmented" role="tablist" aria-label={label} style={{ '--segments': options.length } as never}>
      {/* The raised piece slides to the chosen segment, as the system's segmented control does. */}
      <span className="pw-segmented-thumb" aria-hidden="true" style={{ transform: `translateX(${index * 100}%)` }} />
      {options.map((option) => (
        <button
          key={option.value}
          ref={(element) => {
            refs.current[option.value] = element;
          }}
          type="button"
          role="tab"
          id={`${idPrefix}-${option.value}`}
          aria-selected={option.value === value}
          aria-controls={option.panelId}
          tabIndex={option.value === value ? 0 : -1}
          className={cx('pw-segment', option.value === value && 'is-selected')}
          onClick={() => onChange(option.value)}
          onKeyDown={onKey}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
