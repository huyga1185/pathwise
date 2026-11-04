import type { InputHTMLAttributes, ReactNode } from 'react';
import { cx } from './cx';

export interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  /** Text next to the box; it is the box's accessible name unless aria-label is given. */
  label: ReactNode;
}

/** A checkbox with its text, 18px box in the route colour (web addition, W03: "Hiện cả xe đã tắt", "Đi giao"). */
export function Checkbox({ label, className, ...rest }: CheckboxProps) {
  return (
    <label className={cx('pw-check', className)}>
      <input type="checkbox" {...rest} />
      <span>{label}</span>
    </label>
  );
}
