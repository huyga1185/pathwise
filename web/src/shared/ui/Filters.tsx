import {
  type CSSProperties,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type ReactNode,
  type RefObject,
} from 'react';
import { createPortal } from 'react-dom';
import { useExit } from '@/shared/hooks/useExit';
import {
  CalendarIcon,
  CheckIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  ChevronsLeftIcon,
  ChevronsRightIcon,
  ChevronUpDownIcon,
} from './icons';
import { cx } from './cx';

/**
 * A panel that opens from its button and closes on a click outside or Escape, returning focus to the button (HIG
 * Popovers; WWDC25 219: it "pops open… right where you just tapped").
 */
function usePopover() {
  const [open, setOpen] = useState(false);
  const presence = useExit(open, 140);
  const anchor = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open) return undefined;
    const close = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!panel.current?.contains(target) && !anchor.current?.contains(target)) setOpen(false);
    };
    document.addEventListener('pointerdown', close);
    return () => document.removeEventListener('pointerdown', close);
  }, [open]);

  const close = (focusButton: boolean) => {
    setOpen(false);
    if (focusButton) anchor.current?.focus();
  };
  return { open, setOpen, presence, anchor, panel, close };
}

/**
 * The panel floats over the page on document.body, so it never pushes or scrolls the content under it (a menu in the
 * last row of a table scrolled the table: HIG Menus and Popovers float above the content). Placed against its button:
 * below it, or above when there is no room below (or `above`); `align` its start or its centre with the button; kept
 * 8px inside the window; following the button when the page scrolls.
 */
function Panel({
  presence,
  anchorRef,
  panelRef,
  className,
  above = false,
  align = 'start',
  children,
  ...rest
}: {
  presence: 'open' | 'closing' | 'closed';
  anchorRef: RefObject<HTMLButtonElement | null>;
  panelRef: RefObject<HTMLDivElement | null>;
  className: string;
  above?: boolean;
  align?: 'start' | 'center';
  children: ReactNode;
  id?: string;
  role?: string;
  'aria-label'?: string;
  onKeyDown?: (event: ReactKeyboardEvent<HTMLDivElement>) => void;
}) {
  const shown = presence !== 'closed';
  useLayoutEffect(() => {
    if (!shown) return undefined;
    const place = () => {
      const button = anchorRef.current;
      const element = panelRef.current;
      if (!button || !element) return;
      const a = button.getBoundingClientRect();
      const width = element.offsetWidth;
      const height = element.offsetHeight;
      const room = document.documentElement.clientWidth;
      const start = align === 'center' ? a.left + a.width / 2 - width / 2 : a.left;
      const left = Math.max(8, Math.min(start, room - 8 - width));
      const upward = above || (a.bottom + 8 + height > window.innerHeight - 8 && a.top - 8 - height > 8);
      element.style.left = `${Math.round(left)}px`;
      element.style.top = `${Math.round(upward ? a.top - 8 - height : a.bottom + 8)}px`;
      element.style.transformOrigin = `${align === 'center' ? 'center' : 'left'} ${upward ? 'bottom' : 'top'}`;
    };
    place();
    window.addEventListener('scroll', place, true);
    window.addEventListener('resize', place);
    return () => {
      window.removeEventListener('scroll', place, true);
      window.removeEventListener('resize', place);
    };
  }, [shown, anchorRef, panelRef, above, align]);
  if (!shown) return null;
  const open = presence === 'open';
  return createPortal(
    <div
      ref={panelRef}
      className={cx('pw-popover', className, !open && 'is-closing')}
      {...(open ? rest : { 'aria-hidden': true })}
      inert={!open}
    >
      {children}
    </div>,
    document.body,
  );
}

/**
 * A pop-up button (HIG Pop-up buttons): a flat list of mutually exclusive choices; the button shows the current one so
 * people can predict the options, and opens a menu where the current choice has a checkmark (HIG Menus: "use a
 * checkmark to show that an attribute is currently in effect"). A long menu of generated items scrolls (HIG Menus:
 * "dynamically generated content… a long menu is fine, and scrolling is acceptable"). `above` opens it upward, for a
 * button near the bottom of the window.
 */
export function PopUpButton({
  menuLabel,
  buttonLabel,
  className,
  value,
  choices,
  above = false,
  onChange,
  children,
}: {
  /** The menu's accessible name. */
  menuLabel: string;
  /** The button's accessible name, including the current choice. */
  buttonLabel: string;
  className: string;
  value: string;
  choices: readonly { value: string; label: string }[];
  above?: boolean;
  onChange: (value: string) => void;
  children: ReactNode;
}) {
  const { open, setOpen, presence, anchor, panel, close } = usePopover();
  const menuId = useId();

  useEffect(() => {
    if (!open) return;
    const items = panel.current?.querySelectorAll<HTMLElement>('[role="menuitemradio"]');
    const checked = panel.current?.querySelector<HTMLElement>('[aria-checked="true"]');
    const target = checked ?? items?.[0];
    target?.focus();
    target?.scrollIntoView?.({ block: 'nearest' });
  }, [open, panel]);

  const onKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    const items = [...(panel.current?.querySelectorAll<HTMLElement>('[role="menuitemradio"]') ?? [])];
    const index = items.indexOf(document.activeElement as HTMLElement);
    const move = { ArrowDown: index + 1, ArrowUp: index - 1, Home: 0, End: items.length - 1 }[event.key];
    if (event.key === 'Escape') {
      event.preventDefault();
      close(true);
    } else if (move !== undefined) {
      event.preventDefault();
      items[(move + items.length) % items.length]?.focus();
    } else if (event.key === 'Tab') {
      close(false);
    }
  };

  return (
    <span className="pw-filter-wrap">
      <button
        ref={anchor}
        type="button"
        className={className}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        aria-label={buttonLabel}
        onClick={() => setOpen((was) => !was)}
      >
        {children}
      </button>
      <Panel
        presence={presence}
        anchorRef={anchor}
        panelRef={panel}
        className="pw-menu"
        above={above}
        id={menuId}
        role="menu"
        aria-label={menuLabel}
        onKeyDown={onKeyDown}
      >
        {choices.map((choice) => {
          const checked = choice.value === value;
          return (
            <button
              key={choice.value}
              type="button"
              role="menuitemradio"
              aria-checked={checked}
              tabIndex={-1}
              className="pw-menu-item"
              onClick={() => {
                onChange(choice.value);
                close(true);
              }}
            >
              <span className="pw-menu-check" aria-hidden="true">
                {checked ? <CheckIcon /> : null}
              </span>
              {choice.label}
            </button>
          );
        })}
      </Panel>
    </span>
  );
}

/** A filter as a pop-up button after an introductory label ("Trạng thái: Tất cả", HIG Pop-up buttons). */
export function PopUpFilter({
  label,
  value,
  emptyText,
  options,
  onChange,
}: {
  label: string;
  value?: string;
  emptyText: string;
  options: readonly { value: string; label: string }[];
  onChange: (value: string | undefined) => void;
}) {
  const choices = [{ value: '', label: emptyText }, ...options];
  const current = choices.find((choice) => choice.value === (value ?? '')) ?? choices[0]!;
  return (
    <PopUpButton
      menuLabel={label}
      buttonLabel={`${label}: ${current.label}`}
      className={cx('pw-filter', value && 'is-set')}
      value={current.value}
      choices={choices}
      onChange={(next) => onChange(next || undefined)}
    >
      <span className="pw-filter-label">{label}:</span>
      <span className="pw-filter-value">{current.label}</span>
      <ChevronUpDownIcon />
    </PopUpButton>
  );
}

/**
 * "Trang 4/12": opens a small panel above it with the first page, a field for the page number and the last page
 * (owner's design, 2026-11-17). Digits only (HIG Text fields: "number formatter"); Enter goes there and closes, a page
 * out of range puts the field back to the current page; Escape or a click outside closes.
 */
export function PageJump({
  page,
  pages,
  buttonText,
  buttonLabel,
  fieldLabel,
  firstLabel,
  lastLabel,
  onPage,
}: {
  /** 0-based. */
  page: number;
  pages: number;
  buttonText: string;
  buttonLabel: string;
  fieldLabel: string;
  firstLabel: string;
  lastLabel: string;
  onPage: (page: number) => void;
}) {
  const { open, setOpen, presence, anchor, panel, close } = usePopover();
  const dialogId = useId();
  const field = useRef<HTMLInputElement>(null);
  const [draft, setDraft] = useState(String(page + 1));
  // Closing always puts focus back on the button that opened the panel (the ARIA dialog pattern; WCAG 2.4.3 Focus
  // Order), so keyboard and screen reader users keep their place.
  const done = () => close(true);

  useEffect(() => {
    if (!open) return;
    field.current?.focus();
    field.current?.select();
  }, [open]);

  const go = (target: number) => {
    onPage(target);
    done();
  };
  const commit = () => {
    const wanted = Number.parseInt(draft, 10) - 1;
    if (Number.isInteger(wanted) && wanted >= 0 && wanted < pages) {
      if (wanted !== page) go(wanted);
      else done();
    } else {
      setDraft(String(page + 1));
      field.current?.select();
    }
  };

  return (
    <span className="pw-filter-wrap">
      <button
        ref={anchor}
        type="button"
        className="pw-pager-page"
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls={open ? dialogId : undefined}
        aria-label={buttonLabel}
        onClick={() => {
          if (!open) setDraft(String(page + 1));
          setOpen((was) => !was);
        }}
      >
        {buttonText}
      </button>
      <Panel
        presence={presence}
        anchorRef={anchor}
        panelRef={panel}
        className="pw-page-jump"
        above
        align="center"
        id={dialogId}
        role="dialog"
        aria-label={buttonLabel}
        onKeyDown={(event) => {
          if (event.key === 'Escape') {
            event.preventDefault();
            done();
          }
        }}
      >
        <button
          type="button"
          className="pw-pager-step"
          aria-label={firstLabel}
          title={firstLabel}
          aria-disabled={page === 0}
          onClick={() => page > 0 && go(0)}
        >
          <ChevronsLeftIcon />
        </button>
        <input
          ref={field}
          className="pw-page-jump-field"
          type="text"
          inputMode="numeric"
          enterKeyHint="go"
          autoComplete="off"
          aria-label={fieldLabel}
          value={draft}
          size={Math.max(2, String(pages).length)}
          style={{ '--digits': Math.max(2, String(pages).length) } as CSSProperties}
          onChange={(event) => setDraft(event.target.value.replace(/\D/g, '').slice(0, String(pages).length))}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault();
              commit();
            }
          }}
        />
        <button
          type="button"
          className="pw-pager-step"
          aria-label={lastLabel}
          title={lastLabel}
          aria-disabled={page === pages - 1}
          onClick={() => page < pages - 1 && go(pages - 1)}
        >
          <ChevronsRightIcon />
        </button>
      </Panel>
    </span>
  );
}

const WEEKDAYS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
const WEEKDAY_NAMES = ['Chủ Nhật', 'Thứ Hai', 'Thứ Ba', 'Thứ Tư', 'Thứ Năm', 'Thứ Sáu', 'Thứ Bảy'];

/** A local date as "YYYY-MM-DD" (no timezone involved: calendar days only). */
function iso(year: number, month: number, day: number): string {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
}
function parse(value: string): { year: number; month: number; day: number } {
  const [year, month, day] = value.split('-').map(Number);
  return { year: year ?? 1970, month: (month ?? 1) - 1, day: day ?? 1 };
}
/** "YYYY-MM-DD" moved by `days` calendar days. */
function addDays(value: string, days: number): string {
  const { year, month, day } = parse(value);
  const date = new Date(Date.UTC(year, month, day + days));
  return iso(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
}
/** "YYYY-MM-DD" moved by `months` months, the day kept inside the new month. */
function addMonths(value: string, months: number): string {
  const { year, month, day } = parse(value);
  const last = new Date(Date.UTC(year, month + months + 1, 0)).getUTCDate();
  const date = new Date(Date.UTC(year, month + months, Math.min(day, last)));
  return iso(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
}
function shortDate(value: string): string {
  const { year, month, day } = parse(value);
  return `${String(day).padStart(2, '0')}/${String(month + 1).padStart(2, '0')}/${year}`;
}
function dayName(value: string): string {
  const { year, month, day } = parse(value);
  const weekday = new Date(Date.UTC(year, month, day)).getUTCDay();
  return `${WEEKDAY_NAMES[weekday]}, ${day} tháng ${month + 1}, ${year}`;
}

/**
 * A compact date picker (HIG Pickers: "Use a compact date picker when space is constrained. The compact style displays
 * a button that shows the current value in your app's accent color. When people tap the button, the date picker opens
 * a modal view, providing access to a familiar calendar-style editor… before tapping outside the view to confirm").
 * As there, a chosen day applies at once and the calendar stays open until a click outside it or Escape; today is in
 * the accent colour, the chosen day a filled accent circle. "Mọi ngày" removes the date (and closes).
 */
export function DatePickerFilter({
  label,
  value,
  emptyText,
  today,
  clearText,
  previousMonthText,
  nextMonthText,
  onChange,
}: {
  label: string;
  value?: string;
  emptyText: string;
  /** Today in the store's timezone, "YYYY-MM-DD". */
  today: string;
  clearText: string;
  previousMonthText: string;
  nextMonthText: string;
  onChange: (value: string | undefined) => void;
}) {
  const { open, setOpen, presence, anchor, panel, close } = usePopover();
  const dialogId = useId();
  const titleId = useId();
  // The day with keyboard focus; the month shown is its month.
  const [focusDay, setFocusDay] = useState(value ?? today);
  const grid = useRef<HTMLDivElement>(null);
  const moved = useRef(false);

  useEffect(() => {
    if (!open) return;
    if (!moved.current) {
      grid.current?.querySelector<HTMLElement>('[tabindex="0"]')?.focus();
      return;
    }
    grid.current?.querySelector<HTMLElement>(`[data-day="${focusDay}"]`)?.focus();
  }, [focusDay, open]);

  const { year, month } = parse(focusDay);
  const first = new Date(Date.UTC(year, month, 1));
  const lead = (first.getUTCDay() + 6) % 7; // Monday first
  const days = new Date(Date.UTC(year, month + 1, 0)).getUTCDate();
  const cells: Array<string | null> = [
    ...Array.from({ length: lead }, () => null),
    ...Array.from({ length: days }, (_, index) => iso(year, month, index + 1)),
  ];
  while (cells.length % 7) cells.push(null);

  const choose = (day: string) => {
    moved.current = true;
    setFocusDay(day);
    onChange(day);
  };
  const go = (day: string) => {
    moved.current = true;
    setFocusDay(day);
  };
  const onGridKey = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    const step: Record<string, () => string> = {
      ArrowLeft: () => addDays(focusDay, -1),
      ArrowRight: () => addDays(focusDay, 1),
      ArrowUp: () => addDays(focusDay, -7),
      ArrowDown: () => addDays(focusDay, 7),
      PageUp: () => addMonths(focusDay, -1),
      PageDown: () => addMonths(focusDay, 1),
      Home: () => addDays(focusDay, -((parse(focusDay).day + lead - 1) % 7)),
      End: () => addDays(focusDay, 6 - ((parse(focusDay).day + lead - 1) % 7)),
    };
    const next = step[event.key];
    if (next) {
      event.preventDefault();
      go(next());
    }
  };
  const onPanelKey = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    if (event.key === 'Escape') {
      event.preventDefault();
      close(true);
    }
  };

  const monthTitle = `Tháng ${month + 1}, ${year}`;
  return (
    <span className="pw-filter-wrap">
      <button
        ref={anchor}
        type="button"
        className={cx('pw-filter', value && 'is-set')}
        aria-haspopup="dialog"
        aria-expanded={open}
        aria-controls={open ? dialogId : undefined}
        aria-label={`${label}: ${value ? shortDate(value) : emptyText}`}
        onClick={() => {
          moved.current = false;
          if (!open) setFocusDay(value ?? today);
          setOpen((was) => !was);
        }}
      >
        <CalendarIcon />
        <span className="pw-filter-label">{label}:</span>
        <span className="pw-filter-value pw-filter-date">{value ? shortDate(value) : emptyText}</span>
      </button>
      <Panel
        presence={presence}
        anchorRef={anchor}
        panelRef={panel}
        className="pw-calendar"
        id={dialogId}
        role="dialog"
        aria-label={label}
        onKeyDown={onPanelKey}
      >
        <div className="pw-calendar-head">
          <strong id={titleId} aria-live="polite">
            {monthTitle}
          </strong>
          <button
            type="button"
            className="pw-calendar-nav"
            aria-label={previousMonthText}
            onClick={() => go(addMonths(focusDay, -1))}
          >
            <ChevronLeftIcon />
          </button>
          <button
            type="button"
            className="pw-calendar-nav"
            aria-label={nextMonthText}
            onClick={() => go(addMonths(focusDay, 1))}
          >
            <ChevronRightIcon />
          </button>
        </div>
        <div className="pw-calendar-grid" role="grid" aria-labelledby={titleId} ref={grid} onKeyDown={onGridKey}>
          <div role="row" className="pw-calendar-row">
            {WEEKDAYS.map((name) => (
              <span key={name} role="columnheader" className="pw-calendar-weekday">
                {name}
              </span>
            ))}
          </div>
          {Array.from({ length: cells.length / 7 }, (_, week) => (
            <div role="row" key={week} className="pw-calendar-row">
              {cells.slice(week * 7, week * 7 + 7).map((day, index) =>
                day ? (
                  <span role="gridcell" key={day} aria-selected={day === value}>
                    <button
                      type="button"
                      data-day={day}
                      tabIndex={day === focusDay ? 0 : -1}
                      className={cx('pw-calendar-day', day === value && 'is-selected', day === today && 'is-today')}
                      aria-label={dayName(day)}
                      aria-current={day === today ? 'date' : undefined}
                      onClick={() => choose(day)}
                    >
                      {parse(day).day}
                    </button>
                  </span>
                ) : (
                  <span role="gridcell" key={`empty-${index}`} />
                ),
              )}
            </div>
          ))}
        </div>
        {value ? (
          <div className="pw-calendar-foot">
            <button
              type="button"
              className="pw-calendar-link"
              onClick={() => {
                onChange(undefined);
                close(true);
              }}
            >
              {clearText}
            </button>
          </div>
        ) : null}
      </Panel>
    </span>
  );
}

/** Placeholder rows while a list loads (HIG Loading: "Show something as soon as possible… placeholder"). */
export function PlaceholderRows({ rows = 6, label }: { rows?: number; label: string }) {
  return (
    <div className="pw-placeholder" role="status" aria-label={label}>
      {Array.from({ length: rows }, (_, index) => (
        <div key={index} className="pw-placeholder-row" aria-hidden="true">
          <span style={{ width: `${46 - (index % 3) * 8}%` }} />
          <span style={{ width: `${24 + (index % 2) * 6}%` }} />
        </div>
      ))}
    </div>
  );
}
