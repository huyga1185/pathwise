import type { ButtonHTMLAttributes, ReactNode, Ref } from 'react';
import { cx } from './cx';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** primary: one per screen. danger: destructive. success / signal: driver result buttons only. */
  variant?: 'primary' | 'secondary' | 'quiet' | 'danger' | 'success' | 'signal';
  /** md 36px (web) · lg 48px · xl 56px. */
  size?: 'md' | 'lg' | 'xl';
  /**
   * A request runs: the button shows an activity indicator within it (HIG Buttons: "Configure a button to display an
   * activity indicator when you need to provide feedback about an action that doesn't instantly complete";
   * APPLE_CHECKLIST B-4), takes no press, and says so (aria-busy, aria-disabled). The indicator and `busyLabel`
   * replace the label only after a moment (0.3 s, CSS), so a quick answer changes nothing on screen.
   */
  busy?: boolean;
  /** The label while busy ("Đang tìm…"; HIG Buttons: "display a different label alongside the activity indicator"). */
  busyLabel?: ReactNode;
  block?: boolean;
  ref?: Ref<HTMLButtonElement>;
}

/** Design system Button (brand/design-system/components.d.ts). */
export function Button({
  variant = 'secondary',
  size = 'md',
  busy,
  busyLabel,
  block,
  className,
  type = 'button',
  disabled,
  children,
  onClick,
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      type={type}
      disabled={disabled}
      // Not disabled while busy: dimming it for a quick request made it flash. A press does nothing (a submit button
      // does not submit again).
      aria-disabled={busy ? 'true' : rest['aria-disabled']}
      aria-busy={busy ? 'true' : rest['aria-busy']}
      onClick={busy ? (event) => event.preventDefault() : onClick}
      className={cx(
        'pw-btn',
        `pw-btn-${variant}`,
        `pw-btn-${size}`,
        block && 'pw-btn-block',
        busy && 'is-busy',
        className,
      )}
    >
      {busy ? (
        // Both labels in one cell: the button keeps its width when one gives way to the other.
        <span className="pw-btn-busy">
          <span className="pw-btn-busy-idle" aria-hidden="true">
            {children}
          </span>
          <span className="pw-btn-busy-now">
            <Spinner />
            {busyLabel ?? children}
          </span>
        </span>
      ) : (
        children
      )}
    </button>
  );
}

/** The activity indicator's eight spokes, turning (ui.css). */
function Spinner() {
  return (
    <svg className="pw-btn-spinner" viewBox="0 0 24 24" width="16" height="16" aria-hidden="true">
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
  );
}

/** Classes of a Button, for links that look like one (navigation). */
export function buttonClass(
  variant: ButtonProps['variant'] = 'secondary',
  size: ButtonProps['size'] = 'md',
  block = false,
): string {
  return cx('pw-btn', `pw-btn-${variant}`, `pw-btn-${size}`, block && 'pw-btn-block');
}
