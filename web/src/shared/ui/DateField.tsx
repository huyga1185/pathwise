import { useId, type ReactNode } from 'react';
import { vi } from '@/shared/i18n/vi';
import { cx } from './cx';
import { DatePickerFilter } from './Filters';

export interface DateFieldProps {
  label: string;
  /** "YYYY-MM-DD" or "". */
  value: string;
  onChange: (value: string) => void;
  /** Today in the store's timezone, "YYYY-MM-DD": where the calendar opens when there is no date. */
  today: string;
  required?: boolean;
  hint?: ReactNode;
  error?: ReactNode;
  className?: string;
}

/**
 * A date in a form (OWNER_UI_REDESIGN G4.4): a compact date picker (HIG Date pickers: "a button that shows the current
 * value in your app's accent color. When people tap the button, the date picker opens a modal view… a familiar
 * calendar-style editor"). The calendar is the filters' one: a popover on a wide window, inline on a phone (§8.11),
 * "Tháng ›" for the month and year wheels (§8.12). A required date has no clear button.
 */
export function DateField({ label, value, onChange, today, required, hint, error, className }: DateFieldProps) {
  const id = useId();
  const hintId = `${id}-hint`;
  const errorId = `${id}-err`;
  const describedBy = [error ? errorId : null, hint ? hintId : null].filter(Boolean).join(' ') || undefined;
  return (
    <div className={cx('pw-field', 'pw-date-field', error && 'pw-field-error', className)}>
      <label className="pw-field-label" htmlFor={id}>
        {label}
        {required ? (
          <span aria-hidden="true" className="pw-field-req">
            {' *'}
          </span>
        ) : null}
      </label>
      <DatePickerFilter
        label={label}
        value={value || undefined}
        emptyText={vi.dateField.empty}
        today={today}
        clearText={required ? undefined : vi.dateField.clear}
        previousMonthText={vi.plans.previousMonth}
        nextMonthText={vi.plans.nextMonth}
        field={{ id, invalid: Boolean(error), describedBy, required }}
        onChange={(next) => onChange(next ?? '')}
      />
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
