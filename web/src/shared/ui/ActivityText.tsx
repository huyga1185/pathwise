import { cx } from './cx';

/**
 * A task running, said in place of a static label: the activity indicator turning beside a short description (HIG
 * Progress indicators: an indeterminate task uses an activity indicator; "Keep progress indicators moving so people
 * know something is continuing to happen"; "display a description that provides additional context… Avoid vague
 * terms like loading"). The spokes are drawing only; the words are the text.
 */
export function ActivityText({ text, className }: { text: string; className?: string }) {
  return (
    <span className={cx('pw-activity-text', className)}>
      <svg viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
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
      {text}
    </span>
  );
}
