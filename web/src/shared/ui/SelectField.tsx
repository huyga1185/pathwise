import { useId, type ReactNode, type SelectHTMLAttributes } from 'react';
import { cx } from './cx';

export interface SelectOption {
  value: string;
  label: string;
}

export interface SelectFieldProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: ReactNode;
  options: readonly SelectOption[];
  /** First, empty choice ("Chọn đơn vị") for a required field without a default. */
  placeholder?: string;
  hint?: ReactNode;
  /** Error sentence (UX_BRIEF §6); turns the border danger. */
  error?: ReactNode;
}

/**
 * A choice in a form (web addition, W03): the native select styled like TextField, with the same label, hint, error
 * and aria-describedby. Prototype "Xe" (unit) and "Thành viên" (role).
 */
export function SelectField({
  label,
  options,
  placeholder,
  hint,
  error,
  id,
  className,
  required,
  ...rest
}: SelectFieldProps) {
  const autoId = useId();
  const selectId = id ?? autoId;
  const hintId = `${selectId}-hint`;
  const errorId = `${selectId}-err`;
  const describedBy = [error ? errorId : null, hint ? hintId : null].filter(Boolean).join(' ') || undefined;
  return (
    <div className={cx('pw-field', error && 'pw-field-error', className)}>
      <label className="pw-field-label" htmlFor={selectId}>
        {label}
        {required ? (
          <span aria-hidden="true" className="pw-field-req">
            {' *'}
          </span>
        ) : null}
      </label>
      <select
        {...rest}
        id={selectId}
        required={required}
        className="pw-field-input"
        aria-invalid={error ? 'true' : undefined}
        aria-describedby={describedBy}
      >
        {placeholder !== undefined ? <option value="">{placeholder}</option> : null}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {error ? (
        <p id={errorId} className="pw-field-msg pw-field-msg-error">
          {error}
        </p>
      ) : null}
      {hint ? (
        <p id={hintId} className="pw-field-msg">
          {hint}
        </p>
      ) : null}
    </div>
  );
}
