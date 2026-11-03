import { useRef, type ClipboardEvent, type KeyboardEvent } from 'react';

export interface OtpInputProps {
  /** Up to `length` digits. */
  value: string;
  onChange(value: string): void;
  length?: number;
  label: string;
  invalid?: boolean;
  describedBy?: string;
  disabled?: boolean;
}

/**
 * One box per digit (prototype "Xác thực email"): typing moves to the next box, Backspace on an empty box goes back,
 * arrows move, pasting or autofilling a code anywhere fills every box.
 */
export function OtpInput({ value, onChange, length = 6, label, invalid, describedBy, disabled }: OtpInputProps) {
  const refs = useRef<Array<HTMLInputElement | null>>([]);
  const digits = Array.from({ length }, (_, i) => value[i] ?? '');
  const focus = (index: number) => {
    const box = refs.current[Math.max(0, Math.min(length - 1, index))];
    box?.focus();
    box?.select();
  };
  const setAt = (index: number, digit: string) => {
    const next = digits.slice();
    next[index] = digit;
    onChange(next.join('').slice(0, length));
  };
  const fill = (text: string, start: number) => {
    const clean = text.replace(/\D/g, '');
    if (!clean) return;
    const from = clean.length >= length ? 0 : start;
    const next = digits.slice();
    for (let k = 0; k < clean.length && from + k < length; k++) next[from + k] = clean.charAt(k);
    onChange(next.join(''));
    focus(Math.min(length - 1, from + clean.length));
  };
  const onKeyDown = (index: number) => (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Backspace' && !digits[index] && index > 0) {
      event.preventDefault();
      setAt(index - 1, '');
      focus(index - 1);
    } else if (event.key === 'ArrowLeft' && index > 0) {
      event.preventDefault();
      focus(index - 1);
    } else if (event.key === 'ArrowRight' && index < length - 1) {
      event.preventDefault();
      focus(index + 1);
    }
  };
  const onPaste = (index: number) => (event: ClipboardEvent<HTMLInputElement>) => {
    const text = event.clipboardData.getData('text');
    if (/\d/.test(text)) {
      event.preventDefault();
      fill(text, index);
    }
  };
  return (
    <fieldset
      className={invalid ? 'pw-otp-error' : undefined}
      aria-describedby={describedBy}
      style={{ border: 0, margin: 0, padding: 0, minWidth: 0 }}
    >
      <legend className="pw-field-label" style={{ padding: 0, marginBottom: 8 }}>
        {label}
      </legend>
      <div className="pw-otp">
        {digits.map((digit, index) => (
          <input
            key={index}
            ref={(element) => {
              refs.current[index] = element;
            }}
            className="pw-otp-box"
            type="text"
            inputMode="numeric"
            pattern="[0-9]*"
            maxLength={length}
            autoComplete={index === 0 ? 'one-time-code' : 'off'}
            aria-label={`Chữ số ${index + 1} trên ${length}`}
            aria-invalid={invalid ? 'true' : undefined}
            disabled={disabled}
            value={digit}
            onFocus={(event) => event.target.select()}
            onKeyDown={onKeyDown(index)}
            onPaste={onPaste(index)}
            onChange={(event) => {
              const text = event.target.value.replace(/\D/g, '');
              if (text.length >= length) {
                fill(text, 0);
                return;
              }
              const digitTyped = text.charAt(text.length - 1);
              setAt(index, digitTyped);
              if (digitTyped && index < length - 1) focus(index + 1);
            }}
          />
        ))}
      </div>
    </fieldset>
  );
}
