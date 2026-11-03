import { useId, type InputHTMLAttributes, type ReactNode, type TextareaHTMLAttributes } from 'react';
import { cx } from './cx';

export interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'size'> {
  label: ReactNode;
  hint?: ReactNode;
  /** Error sentence (UX_BRIEF §6); turns the border danger. */
  error?: ReactNode;
  /** Web addition: icon inside the field, before the text. */
  startIcon?: ReactNode;
  /** Web addition: control inside the field, after the text (show password). */
  endAction?: ReactNode;
  /** Web addition: lg is 48px tall with 16px text (account screens). */
  size?: 'md' | 'lg';
  /** A textarea instead of a one-line input (design system prop). */
  multiline?: boolean;
}

/** Design system TextField: label always above, hint and error below, linked by aria-describedby. */
export function TextField({
  label,
  hint,
  error,
  startIcon,
  endAction,
  size = 'md',
  multiline,
  id,
  className,
  required,
  ...rest
}: TextFieldProps) {
  const autoId = useId();
  const inputId = id ?? autoId;
  const hintId = `${inputId}-hint`;
  const errorId = `${inputId}-err`;
  const describedBy = [error ? errorId : null, hint ? hintId : null].filter(Boolean).join(' ') || undefined;
  return (
    <div className={cx('pw-field', error && 'pw-field-error', size === 'lg' && 'pw-field-lg', className)}>
      <label className="pw-field-label" htmlFor={inputId}>
        {label}
        {required ? (
          <span aria-hidden="true" className="pw-field-req">
            {' *'}
          </span>
        ) : null}
      </label>
      <div className={cx('pw-field-control', startIcon && 'has-start', endAction && 'has-end')}>
        {startIcon ? (
          <span className="pw-field-start" aria-hidden="true">
            {startIcon}
          </span>
        ) : null}
        {multiline ? (
          <textarea
            {...(rest as TextareaHTMLAttributes<HTMLTextAreaElement>)}
            id={inputId}
            rows={3}
            required={required}
            className="pw-field-input"
            aria-invalid={error ? 'true' : undefined}
            aria-describedby={describedBy}
          />
        ) : (
          <input
            {...rest}
            id={inputId}
            required={required}
            className="pw-field-input"
            aria-invalid={error ? 'true' : undefined}
            aria-describedby={describedBy}
          />
        )}
        {endAction ? <span className="pw-field-end">{endAction}</span> : null}
      </div>
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
