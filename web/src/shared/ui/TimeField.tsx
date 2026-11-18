import { useEffect, useId, useRef, useState, type Ref } from 'react';
import { useExit } from '@/shared/hooks/useExit';
import { useMediaQuery } from '@/shared/hooks/useMediaQuery';
import { vi } from '@/shared/i18n/vi';
import { cx } from './cx';
import { Panel } from './Filters';
import { Wheel } from './Wheel';

const TIME = /^([01]\d|2[0-3]):([0-5]\d)$/;
/** The minute wheel's step (owner, G4-Q2: HIG Pickers "Consider providing less granularity when specifying minutes"). */
const MINUTE_STEP = 5;

/**
 * "830", "8:30", "8h30", "8.30", "0830", "8" → "08:30" / "08:00"; anything else is returned as typed, so the form's
 * check can say what is wrong. Empty stays empty.
 */
export function normalizeTime(raw: string): string {
  const text = raw.trim();
  if (text === '') return '';
  const parts = /^(\d{1,2})\s*[:.hH]\s*(\d{1,2})$/.exec(text);
  let hours: number;
  let minutes: number;
  if (parts) {
    hours = Number(parts[1]);
    minutes = Number(parts[2]);
  } else if (/^\d{1,4}$/.test(text)) {
    const digits = text.length <= 2 ? text : text.padStart(4, '0');
    hours = Number(digits.length <= 2 ? digits : digits.slice(0, 2));
    minutes = digits.length <= 2 ? 0 : Number(digits.slice(2));
  } else {
    return raw;
  }
  if (hours > 23 || minutes > 59) return raw;
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
}

export function isTime(value: string): boolean {
  return TIME.test(value);
}

export interface TimeFieldProps {
  /** "HH:mm" or "" (or what was typed, until it is fixed). */
  value: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
  name?: string;
  ref?: Ref<HTMLInputElement | HTMLButtonElement>;
  /** Its accessible name (a column heading or a label element names it otherwise). */
  'aria-label'?: string;
  'aria-describedby'?: string;
  id?: string;
  invalid?: boolean;
  disabled?: boolean;
  className?: string;
}

/**
 * A time of day, 24-hour, by input (OWNER_UI_REDESIGN G4.5, owner's choice G4-Q1). With a mouse and keyboard a field
 * to type into (HIG Date pickers, macOS textual style: "useful when… you expect people to make specific date and time
 * selections"), the digits tidied to "HH:mm" when it is left. For touch a button showing the time that opens hour and
 * minute wheels (iOS compact style: "a button that shows the current value… opens a modal view"; HIG Pickers, Wheels;
 * minutes in steps of 5), with "Bỏ giờ" to clear it. An empty field opened takes the wheels' time, as the system
 * picker always holds one.
 */
export function TimeField(props: TimeFieldProps) {
  const touch = useMediaQuery('(pointer: coarse)');
  return touch ? <TimeWheels {...props} /> : <TimeText {...props} />;
}

function TimeText({ value, onChange, onBlur, name, ref, id, invalid, disabled, className, ...aria }: TimeFieldProps) {
  return (
    <input
      ref={ref as Ref<HTMLInputElement>}
      id={id}
      name={name}
      className={cx('pw-field-input pw-time-input', className)}
      type="text"
      inputMode="numeric"
      autoComplete="off"
      placeholder="--:--"
      maxLength={5}
      value={value}
      disabled={disabled}
      aria-invalid={invalid ? 'true' : undefined}
      aria-label={aria['aria-label']}
      aria-describedby={aria['aria-describedby']}
      onChange={(event) => onChange(event.target.value)}
      onBlur={() => {
        const tidy = normalizeTime(value);
        if (tidy !== value) onChange(tidy);
        onBlur?.();
      }}
    />
  );
}

function TimeWheels({ value, onChange, onBlur, ref, id, invalid, disabled, className, ...aria }: TimeFieldProps) {
  const anchor = useRef<HTMLButtonElement | null>(null);
  const panel = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const presence = useExit(open, 140);
  const panelId = useId();
  const time = isTime(value) ? value : '08:00';
  const [hours, minutes] = time.split(':').map(Number) as [number, number];
  const hourItems = Array.from({ length: 24 }, (_, h) => ({ value: h, label: String(h).padStart(2, '0') }));
  const minuteValues = Array.from({ length: 60 / MINUTE_STEP }, (_, i) => i * MINUTE_STEP);
  // A time typed elsewhere (or imported) off the step keeps its minute on the wheel.
  if (!minuteValues.includes(minutes)) minuteValues.push(minutes);
  minuteValues.sort((a, b) => a - b);
  const minuteItems = minuteValues.map((m) => ({ value: m, label: String(m).padStart(2, '0') }));
  const set = (h: number, m: number) => onChange(`${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`);

  useEffect(() => {
    if (!open) return undefined;
    panel.current?.querySelector<HTMLElement>('[role="spinbutton"]')?.focus();
    const outside = (event: PointerEvent) => {
      const node = event.target as Node;
      if (!panel.current?.contains(node) && !anchor.current?.contains(node)) setOpen(false);
    };
    document.addEventListener('pointerdown', outside);
    return () => document.removeEventListener('pointerdown', outside);
  }, [open]);

  const close = () => {
    setOpen(false);
    anchor.current?.focus();
    onBlur?.();
  };

  return (
    <>
      <button
        ref={(element) => {
          anchor.current = element;
          if (typeof ref === 'function') ref(element);
          else if (ref) (ref as { current: HTMLButtonElement | null }).current = element;
        }}
        id={id}
        type="button"
        className={cx('pw-field-input pw-time-button', !value && 'is-empty', className)}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls={open ? panelId : undefined}
        aria-invalid={invalid ? 'true' : undefined}
        aria-label={`${aria['aria-label'] ?? ''}${aria['aria-label'] ? ': ' : ''}${value || vi.time.none}`}
        aria-describedby={aria['aria-describedby']}
        disabled={disabled}
        onClick={() => {
          if (!open && !isTime(value)) onChange(time);
          setOpen((was) => !was);
        }}
      >
        {value || '--:--'}
      </button>
      <Panel
        presence={presence}
        anchorRef={anchor}
        panelRef={panel}
        className="pw-time-panel"
        id={panelId}
        role="dialog"
        aria-label={aria['aria-label'] ?? vi.time.title}
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            event.preventDefault();
            event.stopPropagation();
            close();
          }
        }}
      >
        <div className="pw-wheels pw-time-wheels">
          <Wheel label={vi.time.hours} items={hourItems} value={hours} onChange={(h) => set(h, minutes)} />
          <Wheel label={vi.time.minutes} items={minuteItems} value={minutes} onChange={(m) => set(hours, m)} />
        </div>
        <div className="pw-calendar-foot">
          <button
            type="button"
            className="pw-calendar-link"
            onClick={() => {
              onChange('');
              close();
            }}
          >
            {vi.time.clear}
          </button>
        </div>
      </Panel>
    </>
  );
}
