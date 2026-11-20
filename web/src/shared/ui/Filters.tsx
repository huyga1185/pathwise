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
import { formatDateShort } from '@/shared/format';
import { reducedMotion, useExit } from '@/shared/hooks/useExit';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import {
  CalendarIcon,
  CheckIcon,
  ChevronLeftIcon,
  ChevronRightIcon,
  ChevronsLeftIcon,
  ChevronsRightIcon,
  ChevronUpDownIcon,
  ClearIcon,
  SearchIcon,
} from './icons';
import { Button } from './Button';
import { cx } from './cx';
import { Wheel } from './Wheel';

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
export function Panel({
  presence,
  anchorRef,
  panelRef,
  className,
  above = false,
  align = 'start',
  matchWidth = false,
  children,
  ...rest
}: {
  presence: 'open' | 'closing' | 'closed';
  anchorRef: RefObject<HTMLElement | null>;
  panelRef: RefObject<HTMLDivElement | null>;
  className: string;
  above?: boolean;
  align?: 'start' | 'center';
  /** At least as wide as its button (a pop-up menu under a form field). */
  matchWidth?: boolean;
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
      if (matchWidth) element.style.minWidth = `${Math.round(a.width)}px`;
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
  }, [shown, anchorRef, panelRef, above, align, matchWidth]);
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
  above = true,
  onPage,
}: {
  /** The jump panel opens above the button (a pager at the bottom) or below it (a pager pinned at the top). */
  above?: boolean;
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
        above={above}
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
 * the accent colour, the chosen day a filled accent circle. The clear button ("Bỏ chọn ngày") removes the date (and closes); "Tháng 11, 2026 ›" turns the days into month and year wheels.
 */
export function DatePickerFilter({
  label,
  value,
  emptyText,
  today,
  clearText,
  previousMonthText,
  nextMonthText,
  chooseMonthText = 'Chọn tháng và năm',
  monthWheelText = 'Tháng',
  yearWheelText = 'Năm',
  field,
  onChange,
}: {
  label: string;
  value?: string;
  emptyText: string;
  /** Today in the store's timezone, "YYYY-MM-DD". */
  today: string;
  /** The button removing the date; none for a required date. */
  clearText?: string;
  /**
   * A form field (DateField, G4.4) instead of a filter: the button looks like the other fields and shows the date in
   * the accent colour (HIG Date pickers, compact: "a button that shows the current value in your app's accent color").
   */
  field?: { id: string; invalid?: boolean; describedBy?: string; required?: boolean };
  previousMonthText: string;
  nextMonthText: string;
  chooseMonthText?: string;
  monthWheelText?: string;
  yearWheelText?: string;
  onChange: (value: string | undefined) => void;
}) {
  const { open, setOpen, presence, anchor, panel, close } = usePopover();
  // A wide window: a compact date picker, the calendar in a popover from the button (HIG Date pickers, macOS graphical
  // style in a popover). A phone: an inline date picker, the calendar in the page under the filters, pushing the list
  // down (HIG Date pickers: "Inline — … an inline calendar view"; a picker "works well when displayed in context, below
  // … the field"): a popover there could reach under the tab bar or over the title (owner, 2026-11-18, §8.11).
  const wide = useMediaQuery(WIDE_QUERY);
  const dialogId = useId();
  const titleId = useId();
  // The day with keyboard focus; the month shown is its month.
  const [focusDay, setFocusDay] = useState(value ?? today);
  const grid = useRef<HTMLDivElement>(null);
  const moved = useRef(false);
  // Month and year wheels in place of the days (the iOS calendar's "Month Year ›" button, §8.12).
  const [choosing, setChoosing] = useState(false);
  const titleButton = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;
    if (!moved.current) {
      grid.current?.querySelector<HTMLElement>('[tabindex="0"]')?.focus({ preventScroll: !wide });
      // Inline: bring the whole calendar into view, above the tab bar, scrolling only as much as needed (HIG Scroll
      // views: "scroll automatically… only as much as necessary"; ui.css scroll-margin).
      if (!wide) panel.current?.scrollIntoView?.({ block: 'nearest', behavior: reducedMotion() ? 'auto' : 'smooth' });
      return;
    }
    grid.current?.querySelector<HTMLElement>(`[data-day="${focusDay}"]`)?.focus();
  }, [focusDay, open, wide, panel]);

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
    if (event.key !== 'Escape') return;
    event.preventDefault();
    if (choosing) {
      // First back to the days, then (a second Escape) closed.
      setChoosing(false);
      titleButton.current?.focus();
    } else {
      close(true);
    }
  };
  const todayYear = parse(today).year;
  const years = Array.from({ length: 31 }, (_, offset) => todayYear - 20 + offset).map((y) => ({
    value: y,
    label: String(y),
  }));
  const months = Array.from({ length: 12 }, (_, m) => ({ value: m, label: `Tháng ${m + 1}` }));
  // Another month or year keeps the day, the last day of a shorter month at most.
  const toMonth = (nextYear: number, nextMonth: number) =>
    go(addMonths(focusDay, (nextYear - year) * 12 + (nextMonth - month)));

  const monthTitle = `Tháng ${month + 1}, ${year}`;
  const calendar = (
    <>
      <div className="pw-calendar-head">
        <button
          ref={titleButton}
          type="button"
          className={cx('pw-calendar-title', choosing && 'is-open')}
          aria-expanded={choosing}
          aria-describedby={`${titleId}-hint`}
          onClick={() => setChoosing((was) => !was)}
        >
          <span id={titleId} aria-live="polite">
            {monthTitle}
          </span>
          <ChevronRightIcon />
        </button>
        <span id={`${titleId}-hint`} className="pw-visually-hidden">
          {chooseMonthText}
        </span>
        {/* The arrows step a month; hidden while the wheels are shown (as on iOS). */}
        {choosing ? null : (
          <>
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
          </>
        )}
      </div>
      {choosing ? (
        <div className="pw-wheels">
          <Wheel label={monthWheelText} items={months} value={month} onChange={(m) => toMonth(year, m)} />
          <Wheel label={yearWheelText} items={years} value={year} onChange={(y) => toMonth(y, month)} />
        </div>
      ) : (
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
      )}
      {value && !choosing && clearText ? (
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
    </>
  );
  return (
    <span className={cx('pw-filter-wrap', !wide && 'has-inline-calendar')}>
      <button
        ref={anchor}
        type="button"
        id={field?.id}
        className={
          field ? cx('pw-field-input pw-date-button', !value && 'is-empty') : cx('pw-filter', value && 'is-set')
        }
        aria-invalid={field?.invalid ? 'true' : undefined}
        aria-describedby={field?.describedBy}
        aria-haspopup={wide ? 'dialog' : undefined}
        aria-expanded={open}
        aria-controls={open ? dialogId : undefined}
        aria-label={`${label}: ${value ? shortDate(value) : emptyText}`}
        onClick={() => {
          moved.current = false;
          if (!open) {
            setFocusDay(value ?? today);
            setChoosing(false);
          }
          setOpen((was) => !was);
        }}
      >
        {field ? (
          <>
            <span className="pw-date-value">{value ? formatDateShort(value) : emptyText}</span>
            <CalendarIcon />
          </>
        ) : (
          <>
            <CalendarIcon />
            <span className="pw-filter-label">{label}:</span>
            <span className="pw-filter-value pw-filter-date">{value ? shortDate(value) : emptyText}</span>
          </>
        )}
      </button>
      {wide ? (
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
          {calendar}
        </Panel>
      ) : presence !== 'closed' ? (
        <div
          ref={panel}
          id={dialogId}
          role="group"
          aria-label={label}
          className={cx('pw-calendar pw-calendar-inline', presence === 'closing' && 'is-closing')}
          onKeyDown={onPanelKey}
          {...(presence === 'closing' ? { 'aria-hidden': true, inert: true } : {})}
        >
          {calendar}
        </div>
      ) : null}
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

/**
 * "Xoá bộ lọc": pops in beside the filters and back out where it was. While it leaves, the filters re-flow (the date
 * button gets shorter), so it is lifted out of the row at its last place instead of being carried along; it is inert,
 * not disabled, so it does not dim before fading (the disabled style). When a filter changes and the row re-flows
 * while it stays, it glides to its new place (another row too) instead of jumping.
 */
export function ClearFilters({ text, closing, onClear }: { text: string; closing: boolean; onClear: () => void }) {
  const button = useRef<HTMLButtonElement>(null);
  const place = useRef<{ left: number; top: number } | null>(null);
  // Every render: remember where it sits while shown; once it leaves, pin it there.
  useLayoutEffect(() => {
    const element = button.current;
    if (!element) return;
    if (!closing) {
      const was = place.current;
      place.current = { left: element.offsetLeft, top: element.offsetTop };
      // The filters re-flowed and it moved (another row, another place): glide there instead of jumping.
      const dx = was ? was.left - place.current.left : 0;
      const dy = was ? was.top - place.current.top : 0;
      if ((dx || dy) && !reducedMotion()) {
        element.animate([{ transform: `translate(${dx}px, ${dy}px)` }, { transform: 'none' }], {
          duration: 280,
          easing: 'cubic-bezier(0.2, 0.8, 0.2, 1)',
        });
      }
      element.style.removeProperty('position');
      element.style.removeProperty('left');
      element.style.removeProperty('top');
    } else if (place.current) {
      element.style.position = 'absolute';
      element.style.left = `${place.current.left}px`;
      element.style.top = `${place.current.top}px`;
    }
  });
  return (
    <Button
      ref={button}
      variant="quiet"
      className={cx('pw-list-clear', closing && 'is-closing')}
      inert={closing}
      aria-hidden={closing || undefined}
      onClick={onClear}
    >
      {text}
    </Button>
  );
}

/**
 * A search field on a list's filter row (HIG Search fields): a capsule like the filter buttons, a magnifying glass at
 * its leading end, a clear button at its trailing end while it has text, results as people type ("Consider showing
 * results immediately as people type") — 0.3 s after the last key, or at once with Enter. The placeholder says what
 * it searches; the field's name is `label` (an accessible name, as Apple's search fields have no label above).
 */
export function SearchField({
  label,
  placeholder,
  value,
  clearText,
  onSearch,
}: {
  label: string;
  placeholder: string;
  value: string;
  clearText: string;
  onSearch: (value: string) => void;
}) {
  const [text, setText] = useState(value);
  const [last, setLast] = useState(value);
  // Another value from outside (Back, cleared filters): show it.
  if (value !== last) {
    setLast(value);
    setText(value);
  }
  const input = useRef<HTMLInputElement>(null);
  useEffect(() => {
    if (text.trim() === value) return undefined;
    const timer = window.setTimeout(() => onSearch(text.trim()), 300);
    return () => window.clearTimeout(timer);
  }, [text, value, onSearch]);
  return (
    <form
      role="search"
      className={cx('pw-search', text && 'has-text')}
      onSubmit={(event) => {
        event.preventDefault();
        onSearch(text.trim());
      }}
    >
      <SearchIcon />
      <input
        ref={input}
        type="search"
        aria-label={label}
        placeholder={placeholder}
        value={text}
        enterKeyHint="search"
        onChange={(event) => setText(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Escape' && text) {
            event.preventDefault();
            setText('');
            onSearch('');
          }
        }}
      />
      {text ? (
        <button
          type="button"
          className="pw-search-clear"
          aria-label={clearText}
          title={clearText}
          onClick={() => {
            setText('');
            onSearch('');
            input.current?.focus();
          }}
        >
          <ClearIcon />
        </button>
      ) : null}
    </form>
  );
}
