import type { InputHTMLAttributes, ReactNode } from 'react';
import { cx } from './cx';

export interface CheckboxProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> {
  /** Text next to the box: what it sets, not its state; its accessible name unless aria-label is given. None: a box
   * in a list or table row whose column heading says what it is (HIG Toggles: "In an editable checklist, a checkbox
   * can appear without a title"); give it aria-label. */
  label?: ReactNode;
}

/** A checkbox with its text, 18px box in the route colour (web addition, W03: "Hiện cả xe đã tắt", "Đi giao"). */
export function Checkbox({ label, className, ...rest }: CheckboxProps) {
  return (
    <label className={cx('pw-check', className)}>
      <input type="checkbox" {...rest} />
      {label !== undefined ? <span>{label}</span> : null}
    </label>
  );
}
