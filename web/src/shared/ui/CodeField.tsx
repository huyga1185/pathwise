import { useId, useState, type SyntheticEvent } from 'react';
import { cx } from './cx';

export interface CodeFieldProps {
  label: string;
  value: string;
  /** Digits only, at most `length` of them. */
  onChange: (value: string) => void;
  length?: number;
  hint?: string;
  error?: string;
  disabled?: boolean;
}

/**
 * A one-time code: ONE real input drawn as a row of boxes. The input keeps the system behaviour Apple documents for
 * codes (AutoFill on a field whose content type is one-time-code; numeric keyboard; paste, delete, select all), and
 * screen readers see one field with its label; the boxes are drawing only (aria-hidden).
 */
export function CodeField({ label, value, onChange, length = 6, hint, error, disabled }: CodeFieldProps) {
  const inputId = useId();
  const hintId = `${inputId}-hint`;
  const errorId = `${inputId}-err`;
  const describedBy = [error ? errorId : null, hint ? hintId : null].filter(Boolean).join(' ') || undefined;
  const [focused, setFocused] = useState(false);
  // The next box to fill is lit, as the drawn stand-in for the hidden caret.
  const active = focused ? Math.min(value.length, length - 1) : -1;

  // Typing always goes to the end: a caret moved into the middle (click, arrow) returns there; a real selection
  // (select all) is left alone so it can be deleted or replaced.
  const keepCaretAtEnd = (event: SyntheticEvent<HTMLInputElement>) => {
    const input = event.currentTarget;
    if (input.selectionStart === input.selectionEnd && input.selectionStart !== input.value.length) {
      input.setSelectionRange(input.value.length, input.value.length);
    }
  };

  return (
    <div className={cx('pw-field', 'pw-code', error && 'pw-field-error')}>
      <label className="pw-field-label" htmlFor={inputId}>
        {label}
      </label>
      <div className="pw-code-control">
        <div className="pw-code-cells" aria-hidden="true">
          {Array.from({ length }, (_, index) => (
            <span
              key={index}
              className={cx('pw-code-cell', index === active && 'is-active', index < value.length && 'is-filled')}
            >
              {value[index] ?? ''}
            </span>
          ))}
        </div>
        <input
          id={inputId}
          className="pw-code-input"
          type="text"
          inputMode="numeric"
          autoComplete="one-time-code"
          pattern="[0-9]*"
          spellCheck={false}
          value={value}
          disabled={disabled}
          aria-invalid={error ? 'true' : undefined}
          aria-describedby={describedBy}
          // No maxLength: it would cut a pasted "123 456" before the spaces are dropped. Digits are kept, then cut here.
          onChange={(event) => onChange(event.target.value.replace(/\D/g, '').slice(0, length))}
          onFocus={() => setFocused(true)}
          onBlur={() => setFocused(false)}
          onSelect={keepCaretAtEnd}
        />
      </div>
      {/* Always there, so a wrong code said after sending is read out (it was the verify-email page's live message). */}
      <div aria-live="polite">
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
    </div>
  );
}
