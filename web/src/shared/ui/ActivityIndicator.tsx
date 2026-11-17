import { cx } from './cx';

/**
 * Apple's activity indicator: eight spokes turning, for a wait of unknown length (HIG Progress indicators:
 * indeterminate, "Avoid vague terms like loading" — so no visible text, only a name for assistive technologies). It
 * shows after a short delay, so a quick load never flashes it (CSS).
 */
export function ActivityIndicator({ label, className }: { label: string; className?: string }) {
  return (
    <div className={cx('pw-activity-inline', className)} role="status" aria-label={label}>
      <svg viewBox="0 0 24 24" width="24" height="24" aria-hidden="true">
        {Array.from({ length: 8 }, (_, index) => (
          <rect
            key={index}
            x="11"
            y="2"
            width="2"
            height="6"
            rx="1"
            fill="currentColor"
            opacity={(0.25 + (index / 8) * 0.75).toFixed(2)}
            transform={`rotate(${index * 45} 12 12)`}
          />
        ))}
      </svg>
    </div>
  );
}
