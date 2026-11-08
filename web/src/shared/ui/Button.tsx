import type { ButtonHTMLAttributes } from 'react';
import { cx } from './cx';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  /** primary: one per screen. danger: destructive. success / signal: driver result buttons only. */
  variant?: 'primary' | 'secondary' | 'quiet' | 'danger' | 'success' | 'signal';
  /** md 36px (web) · lg 48px · xl 56px. */
  size?: 'md' | 'lg' | 'xl';
  /** Disables the button and sets aria-busy while a request runs. */
  busy?: boolean;
  block?: boolean;
}

/** Design system Button (brand/design-system/components.d.ts). */
export function Button({
  variant = 'secondary',
  size = 'md',
  busy,
  block,
  className,
  type = 'button',
  disabled,
  ...rest
}: ButtonProps) {
  return (
    <button
      {...rest}
      type={type}
      disabled={disabled || busy}
      aria-busy={busy ? 'true' : undefined}
      className={cx('pw-btn', `pw-btn-${variant}`, `pw-btn-${size}`, block && 'pw-btn-block', className)}
    />
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
