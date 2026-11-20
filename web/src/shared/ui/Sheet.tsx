import { useEffect, useId, useLayoutEffect, useRef, useState, type FormEvent, type PointerEvent, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { useExit } from '@/shared/hooks/useExit';
import { useMediaQuery, WIDE_QUERY } from '@/shared/hooks/useMediaQuery';
import { Button } from './Button';
import { cx } from './cx';
import { keepFocusInside } from './Dialog';

/** Focus inside while open (Tab cycles), Escape calls onEscape, focus goes back where it was afterwards. */
function useModalFocus(open: boolean, ref: React.RefObject<HTMLElement | null>, onEscape: () => void) {
  const escape = useRef(onEscape);
  useEffect(() => {
    escape.current = onEscape;
  });
  useEffect(() => {
    if (!open) return undefined;
    const previous = document.activeElement as HTMLElement | null;
    // The marked element, else the first field to fill in, else the first button.
    const root = ref.current;
    const first =
      root?.querySelector<HTMLElement>('[data-autofocus]') ??
      root?.querySelector<HTMLElement>('input:not([disabled]), select:not([disabled]), textarea:not([disabled])') ??
      root?.querySelector<HTMLElement>('button:not([disabled])');
    first?.focus({ preventScroll: true });
    const onKey = (event: KeyboardEvent) => {
      // An Escape a popover inside already handled (a calendar, a menu, a time picker: they prevent its default)
      // closes only that popover, not the sheet around it.
      if (event.key === 'Escape' && !event.defaultPrevented) {
        event.preventDefault();
        escape.current();
      }
      if (event.key === 'Tab' && ref.current) keepFocusInside(ref.current, event);
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.({ preventScroll: true });
    };
  }, [open, ref]);
}

export interface SheetProps {
  open: boolean;
  title: ReactNode;
  children: ReactNode;
  /** The finishing action, a verb: "Thêm", "Lưu", "Gửi". It submits the sheet's form. None: a sheet whose changes
   * apply at once, closed with cancelLabel (give it "Đóng"; HIG Sheets: "The Cancel (or Close) button"). */
  doneLabel?: ReactNode;
  cancelLabel?: ReactNode;
  busy?: boolean;
  doneDisabled?: boolean;
  /** Unsaved changes: swiping the sheet away asks first (action sheet). */
  dirty?: boolean;
  discardLabel?: ReactNode;
  keepEditingLabel?: ReactNode;
  onSubmit?: (event: FormEvent<HTMLFormElement>) => void;
  onClose: () => void;
  /** A later step of a multi-step sheet: the leading button goes back a step instead of cancelling (HIG Sheets: "The
   * Back button lets people navigate to a previous step… Avoid showing all three buttons… together"). Escape and a
   * swipe still close the sheet. */
  onBack?: () => void;
  backLabel?: ReactNode;
  /** A wide window: `large` for content that needs the room (a preview table); a phone's sheet is full width anyway. */
  size?: 'regular' | 'large';
}

/**
 * A sheet for a short task with input (HIG Sheets): a title, "Huỷ" at the leading end of its top bar and the finishing
 * verb at the trailing end ("for sheets with a single view, the Cancel button belongs on the leading edge of the top
 * toolbar… the Done button… trailing edge"; macOS gives no placement, so the same bar on a computer). On a phone it
 * rises from the bottom with a grabber and can be swiped down to close; with unsaved changes the swipe asks first
 * with an action sheet ("If people have unsaved changes… use an action sheet"). On a wide window it is a card in the
 * middle over a dimmed page ("The parent window is dimmed"), sized to its content. "Huỷ" and Escape close without
 * asking ("The Cancel… button dismisses a sheet without saving any changes").
 */
export function Sheet({
  open,
  title,
  children,
  doneLabel,
  cancelLabel = 'Huỷ',
  busy,
  doneDisabled,
  dirty = false,
  discardLabel = 'Bỏ thay đổi',
  keepEditingLabel = 'Tiếp tục sửa',
  onSubmit,
  onClose,
  onBack,
  backLabel = 'Quay lại',
  size = 'regular',
}: SheetProps) {
  const titleId = useId();
  const formId = useId();
  const wide = useMediaQuery(WIDE_QUERY);
  const presence = useExit(open, 220);
  const panel = useRef<HTMLDivElement>(null);
  const [drag, setDrag] = useState(0);
  const start = useRef<{ y: number; time: number } | null>(null);
  const [asking, setAsking] = useState(false);
  useModalFocus(open, panel, () => (asking ? setAsking(false) : onClose()));

  if (presence === 'closed') return null;
  const closing = presence === 'closing';

  const onGrab = (event: PointerEvent<HTMLDivElement>) => {
    if (wide) return;
    if ((event.target as HTMLElement).closest('button')) return;
    start.current = { y: event.clientY, time: performance.now() };
    event.currentTarget.setPointerCapture?.(event.pointerId);
  };
  const onMove = (event: PointerEvent<HTMLDivElement>) => {
    if (!start.current) return;
    setDrag(Math.max(0, event.clientY - start.current.y));
  };
  const onRelease = (event: PointerEvent<HTMLDivElement>) => {
    const begun = start.current;
    start.current = null;
    if (!begun) return;
    const distance = event.clientY - begun.y;
    const fast = distance / Math.max(1, performance.now() - begun.time) > 0.6;
    setDrag(0);
    if (distance > 120 || (fast && distance > 30)) {
      if (dirty) setAsking(true);
      else onClose();
    }
  };

  // On document.body: above the shell (top bar, sidebar, tab bar), outside any stacking context of the page.
  return createPortal(
    <div
      className={cx('pw-sheet-backdrop', closing && 'is-closing', wide ? 'is-wide' : 'is-narrow')}
      onPointerDown={(event) => {
        // A tap on the dimmed page closes a sheet without changes; with changes it asks, like a swipe.
        if (event.target === event.currentTarget) {
          if (dirty) setAsking(true);
          else onClose();
        }
      }}
    >
      <div
        ref={panel}
        className={cx('pw-sheet', size === 'large' && 'is-large', closing && 'is-closing')}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        style={drag ? { transform: `translateY(${drag}px)`, transition: 'none' } : undefined}
        inert={closing}
      >
        <div
          className="pw-sheet-top"
          onPointerDown={onGrab}
          onPointerMove={onMove}
          onPointerUp={onRelease}
          onPointerCancel={onRelease}
        >
          {!wide ? <span className="pw-sheet-grabber" aria-hidden="true" /> : null}
          <div className="pw-sheet-bar">
            <Button variant="quiet" onClick={onBack ?? onClose}>
              {onBack ? backLabel : cancelLabel}
            </Button>
            <h2 id={titleId} className="pw-sheet-title">
              {title}
            </h2>
            {doneLabel ? (
              <Button variant="primary" type="submit" form={formId} busy={busy} disabled={doneDisabled}>
                {doneLabel}
              </Button>
            ) : (
              <span />
            )}
          </div>
        </div>
        <form
          id={formId}
          className="pw-sheet-body"
          noValidate
          onSubmit={(event) => (onSubmit ? onSubmit(event) : event.preventDefault())}
        >
          {children}
        </form>
        {asking ? (
          <ActionSheet
            choices={[
              { label: discardLabel, destructive: true, onSelect: onClose },
              { label: keepEditingLabel, onSelect: () => setAsking(false), cancel: true },
            ]}
            onDismiss={() => setAsking(false)}
          />
        ) : null}
      </div>
    </div>,
    document.body,
  );
}

/**
 * An action sheet on its own, over the page (not inside a sheet): HIG Alerts — "Use an action sheet — not an alert —
 * to offer choices related to an intentional action", like discarding edits when cancelling them.
 */
export function PageActionSheet({
  open,
  choices,
  onDismiss,
  title,
  message,
}: {
  open: boolean;
  title: string;
  message?: string;
  choices: { label: ReactNode; destructive?: boolean; cancel?: boolean; onSelect: () => void }[];
  onDismiss: () => void;
}) {
  useEffect(() => {
    if (!open) return undefined;
    const previous = document.activeElement as HTMLElement | null;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onDismiss();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.({ preventScroll: true });
    };
  }, [open, onDismiss]);
  return open
    ? createPortal(
        <ActionSheet choices={choices} onDismiss={onDismiss} title={title} message={message} />,
        document.body,
      )
    : null;
}

/**
 * Choices about something people just did (HIG Action sheets: "Use an action sheet — not an alert — to offer choices
 * related to an intentional action"): a destructive choice at the top, in red ("Make destructive choices visually
 * prominent… place these buttons at the top"), the way back at the bottom.
 */
function ActionSheet({
  choices,
  onDismiss,
  title,
  message,
}: {
  choices: { label: ReactNode; destructive?: boolean; cancel?: boolean; onSelect: () => void }[];
  onDismiss: () => void;
  /** A short title over the choices (as iOS action sheets); also the dialog's accessible name (WCAG 4.1.2). */
  title?: string;
  message?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const titleId = useId();
  useEffect(() => {
    ref.current?.querySelector<HTMLElement>('button')?.focus();
  }, []);
  return (
    <div
      className="pw-action-sheet-backdrop"
      onPointerDown={(event) => event.target === event.currentTarget && onDismiss()}
    >
      <div
        ref={ref}
        className="pw-action-sheet"
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={title ? titleId : undefined}
      >
        <div className="pw-action-sheet-group">
          {title ? (
            <div className="pw-action-sheet-head">
              <span id={titleId} className="pw-action-sheet-title">
                {title}
              </span>
              {message ? <span className="pw-action-sheet-message">{message}</span> : null}
            </div>
          ) : null}
          {choices
            .filter((choice) => !choice.cancel)
            .map((choice, index) => (
              <button
                key={index}
                type="button"
                className={cx('pw-action-sheet-button', choice.destructive && 'is-destructive')}
                onClick={choice.onSelect}
              >
                {choice.label}
              </button>
            ))}
        </div>
        {choices
          .filter((choice) => choice.cancel)
          .map((choice, index) => (
            <button key={index} type="button" className="pw-action-sheet-button is-cancel" onClick={choice.onSelect}>
              {choice.label}
            </button>
          ))}
      </div>
    </div>
  );
}

export interface AlertProps {
  open: boolean;
  /** What happened or is about to happen, specific (HIG Alerts: "Write a title that clearly and succinctly…"). */
  title: ReactNode;
  /** Only if it adds value. */
  message?: ReactNode;
  /** One or two words, a verb ("Vô hiệu hoá", "Huỷ kế hoạch"). */
  actionLabel: ReactNode;
  cancelLabel?: ReactNode;
  /** Red only for a destructive result people did not deliberately choose (HIG Alerts). */
  destructive?: boolean;
  busy?: boolean;
  onAction: () => void;
  onCancel: () => void;
}

/**
 * An alert, only to confirm something hard to undo (HIG Alerts: "Use alerts sparingly", not "for common, undoable
 * actions"): a title, an optional short message, "Huỷ" on the leading side (never the default, always titled
 * Cancel) and the action on the trailing side; Escape cancels ("Provide alternative ways to cancel an alert").
 */
export function Alert({
  open,
  title,
  message,
  actionLabel,
  cancelLabel = 'Huỷ',
  destructive = false,
  busy,
  onAction,
  onCancel,
}: AlertProps) {
  const titleId = useId();
  const messageId = useId();
  const presence = useExit(open, 160);
  const panel = useRef<HTMLDivElement>(null);
  const actions = useRef<HTMLDivElement>(null);
  // Two buttons in a row; a title too long for its button stacks them, the action on top and Cancel at the
  // bottom (HIG Alerts: "Always place the default button on the trailing side of a row or at the top of a stack.
  // Cancel buttons are typically on the leading side of a row or at the bottom of a stack."). Measured before paint;
  // the order in the DOM follows, so the focus order matches what people see.
  const [stacked, setStacked] = useState(false);
  const [wasOpen, setWasOpen] = useState(open);
  if (open !== wasOpen) {
    setWasOpen(open);
    if (!open) setStacked(false);
  }
  useLayoutEffect(() => {
    if (!open || stacked) return;
    const buttons = actions.current ? [...actions.current.querySelectorAll('button')] : [];
    if (buttons.some((button) => button.scrollWidth > button.clientWidth)) setStacked(true);
  }, [open, stacked, actionLabel, cancelLabel]);
  useModalFocus(open, panel, onCancel);
  if (presence === 'closed') return null;
  const closing = presence === 'closing';
  const cancelButton = (
    <button key="cancel" type="button" className="pw-alert-button" data-autofocus onClick={onCancel}>
      {cancelLabel}
    </button>
  );
  const actionButton = (
    <button
      key="action"
      type="button"
      className={cx('pw-alert-button is-action', destructive && 'is-destructive')}
      disabled={busy}
      aria-busy={busy || undefined}
      onClick={onAction}
    >
      {actionLabel}
    </button>
  );
  return createPortal(
    <div className={cx('pw-alert-backdrop', closing && 'is-closing')}>
      <div
        ref={panel}
        className={cx('pw-alert', closing && 'is-closing')}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={message ? messageId : undefined}
        inert={closing}
      >
        <div className="pw-alert-text">
          <h2 id={titleId} className="pw-alert-title">
            {title}
          </h2>
          {message ? (
            <p id={messageId} className="pw-alert-message">
              {message}
            </p>
          ) : null}
        </div>
        <div ref={actions} className={cx('pw-alert-actions', stacked && 'is-stacked')}>
          {stacked ? [actionButton, cancelButton] : [cancelButton, actionButton]}
        </div>
      </div>
    </div>,
    document.body,
  );
}
