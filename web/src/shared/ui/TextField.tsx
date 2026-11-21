import {
  useId,
  useRef,
  useState,
  type InputEvent,
  type InputHTMLAttributes,
  type ReactNode,
  type Ref,
  type TextareaHTMLAttributes,
} from 'react';
import { vi } from '@/shared/i18n/vi';
import { cx } from './cx';
import { ClearIcon } from './icons';

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
  /**
   * A clear button at the trailing end while the field has text (HIG Text fields, iOS: "Display a Clear button in the
   * trailing end of a text field"). One-line fields without an endAction only; not for passwords.
   */
  clearable?: boolean;
  ref?: Ref<HTMLInputElement>;
}

/** Empties an input the way typing would, so React onChange and react-hook-form see it. */
function clearInput(input: HTMLInputElement) {
  Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set?.call(input, '');
  input.dispatchEvent(new Event('input', { bubbles: true }));
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
  clearable,
  id,
  className,
  required,
  ref,
  onInput,
  ...rest
}: TextFieldProps) {
  const autoId = useId();
  const input = useRef<HTMLInputElement | null>(null);
  const [filled, setFilled] = useState(false);
  const canClear = Boolean(clearable && !multiline && !endAction && !rest.disabled && !rest.readOnly);
  const setInput = (element: HTMLInputElement | null) => {
    input.current = element;
    // An uncontrolled field (react-hook-form) may start with a value.
    if (element && canClear && rest.value === undefined) setFilled(element.value !== '');
    if (typeof ref === 'function') ref(element);
    else if (ref) ref.current = element;
  };
  const handleInput = (event: InputEvent<HTMLInputElement>) => {
    if (canClear) setFilled(event.currentTarget.value !== '');
    onInput?.(event);
  };
  // A controlled field may change without an input event (e.g. filled from a link): read its value.
  const showClear = canClear && (rest.value !== undefined ? String(rest.value) !== '' : filled);
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
      <div className={cx('pw-field-control', startIcon && 'has-start', (endAction || canClear) && 'has-end')}>
        {startIcon ? (
          <span className="pw-field-start" aria-hidden="true">
            {startIcon}
          </span>
        ) : null}
        {multiline ? (
          <textarea
            {...(rest as TextareaHTMLAttributes<HTMLTextAreaElement>)}
            ref={ref as unknown as Ref<HTMLTextAreaElement>}
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
            ref={setInput}
            onInput={handleInput}
            id={inputId}
            required={required}
            className="pw-field-input"
            aria-invalid={error ? 'true' : undefined}
            aria-describedby={describedBy}
          />
        )}
        {endAction ? <span className="pw-field-end">{endAction}</span> : null}
        {showClear ? (
          <span className="pw-field-end">
            {/* Pointer and touch only (keyboard users select and delete): out of the tab order, and the press does not
                take focus from the field, so its blur check does not run. */}
            <button
              type="button"
              className="pw-icon-button pw-field-clear"
              aria-label={vi.form.clear}
              tabIndex={-1}
              onMouseDown={(event) => event.preventDefault()}
              onClick={() => {
                if (!input.current) return;
                clearInput(input.current);
                input.current.focus();
              }}
            >
              <ClearIcon />
            </button>
          </span>
        ) : null}
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
