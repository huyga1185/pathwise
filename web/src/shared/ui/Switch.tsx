import type { ReactNode } from 'react';
import { cx } from './cx';

/**
 * A switch in a list row (HIG Toggles: "Use the switch toggle style only in a list row… the content in the row provides
 * the context"): the row's label on the leading side, the switch on the trailing side; the whole row toggles it. On and
 * off differ by more than colour: the knob moves (HIG Toggles: "Avoid relying solely on different colors").
 */
export function SwitchRow({
  label,
  hint,
  checked,
  disabled,
  onChange,
}: {
  label: ReactNode;
  hint?: ReactNode;
  checked: boolean;
  disabled?: boolean;
  onChange: (checked: boolean) => void;
}) {
  return (
    <button
      type="button"
      role="switch"
      aria-checked={checked}
      disabled={disabled}
      className={cx('pw-switch-row', checked && 'is-on')}
      onClick={() => onChange(!checked)}
    >
      <span className="pw-switch-text">
        <span className="pw-switch-label">{label}</span>
        {hint ? <span className="pw-switch-hint">{hint}</span> : null}
      </span>
      <span className="pw-switch" aria-hidden="true">
        <span className="pw-switch-knob" />
      </span>
    </button>
  );
}
