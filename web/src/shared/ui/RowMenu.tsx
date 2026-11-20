import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent,
  type PointerEvent,
  type ReactNode,
} from 'react';
import { createPortal } from 'react-dom';
import { cx } from './cx';

export interface RowMenuItem {
  label: string;
  icon?: ReactNode;
  /** Red, for a command that destroys or removes something (HIG Context menus: listed last, identified as destructive). */
  destructive?: boolean;
  onSelect: () => void;
}

/**
 * A row's context menu, the same in every list (HIG Context menus: "Support context menus consistently throughout your
 * app"): touch and hold a row on a phone (0.5 s; moving the finger more than 10 px cancels it, for scrolling), Android's
 * long press or a right click. A right click opens it at the pointer; a touch, under the row. The tap that ends a
 * touch-and-hold does not also open the row (`consumeHold`).
 */
export function useRowMenu<Row>() {
  const [menu, setMenu] = useState<{ row: Row; rect: DOMRect } | null>(null);
  const held = useRef(false);
  const hold = useRef<{ timer: number; x: number; y: number } | null>(null);
  const stopHold = () => {
    if (hold.current) window.clearTimeout(hold.current.timer);
    hold.current = null;
  };
  const open = (row: Row, rect: DOMRect) => setMenu({ row, rect });
  const close = () => setMenu(null);
  /** Spread on the row element. */
  const holdHandlers = (row: Row) => ({
    onPointerDown: (event: PointerEvent<HTMLElement>) => {
      if (event.pointerType !== 'touch') return;
      const element = event.currentTarget;
      held.current = false;
      stopHold();
      hold.current = {
        x: event.clientX,
        y: event.clientY,
        timer: window.setTimeout(() => {
          held.current = true;
          hold.current = null;
          setMenu({ row, rect: element.getBoundingClientRect() });
        }, 500),
      };
    },
    onPointerMove: (event: PointerEvent<HTMLElement>) => {
      const start = hold.current;
      if (start && Math.hypot(event.clientX - start.x, event.clientY - start.y) > 10) stopHold();
    },
    onPointerUp: stopHold,
    onPointerCancel: stopHold,
    onContextMenu: (event: MouseEvent<HTMLElement>) => {
      event.preventDefault();
      stopHold();
      const touch = (event.nativeEvent as globalThis.PointerEvent).pointerType === 'touch';
      setMenu({
        row,
        rect: touch
          ? event.currentTarget.getBoundingClientRect()
          : new DOMRect(event.clientX - 8, event.clientY - 8, 0, 0),
      });
    },
  });
  /** On the row's link or button: a click that only ends a touch-and-hold does nothing. */
  const consumeHold = (event: MouseEvent) => {
    if (held.current) {
      event.preventDefault();
      event.stopPropagation();
      held.current = false;
    }
  };
  /** A right click at a point (a table row: DataTable's onRowContextMenu). */
  const openAt = (row: Row, point: { x: number; y: number }) =>
    setMenu({ row, rect: new DOMRect(point.x - 8, point.y - 8, 0, 0) });
  return { menu, open, openAt, close, holdHandlers, consumeHold };
}

/**
 * The menu: a few commands under the row, or above it near the bottom of the screen (HIG Context menus: "Aim for a
 * small number of menu items", "Hide unavailable menu items"; HIG Menus: an ellipsis when more input follows). Closes
 * on a choice, a tap outside, Escape or scrolling; arrows move between items; focus goes to the first item and back
 * where it was afterwards.
 */
export function RowMenu({
  rect,
  label,
  items,
  onClose,
}: {
  rect: DOMRect;
  label: string;
  items: RowMenuItem[];
  onClose: () => void;
}) {
  const menu = useRef<HTMLDivElement>(null);
  // The latest onClose, so the listeners below are set once while the menu is open.
  const close = useRef(onClose);
  useEffect(() => {
    close.current = onClose;
  });
  useEffect(() => {
    const onClose = () => close.current();
    const before = document.activeElement as HTMLElement | null;
    menu.current?.querySelector<HTMLElement>('[role="menuitem"]')?.focus({ preventScroll: true });
    const outside = (event: globalThis.PointerEvent) => {
      if (!menu.current?.contains(event.target as Node)) onClose();
    };
    const key = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    // The finger that opened it is still lifting: listen for taps outside from the next frame.
    const frame = window.requestAnimationFrame(() => document.addEventListener('pointerdown', outside));
    document.addEventListener('keydown', key);
    window.addEventListener('scroll', onClose, { passive: true });
    return () => {
      window.cancelAnimationFrame(frame);
      document.removeEventListener('pointerdown', outside);
      document.removeEventListener('keydown', key);
      window.removeEventListener('scroll', onClose);
      before?.focus?.({ preventScroll: true });
    };
  }, []);
  const onKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    const all = [...(menu.current?.querySelectorAll<HTMLElement>('[role="menuitem"]') ?? [])];
    const index = all.indexOf(document.activeElement as HTMLElement);
    const move = { ArrowDown: index + 1, ArrowUp: index - 1, Home: 0, End: all.length - 1 }[event.key];
    if (move !== undefined) {
      event.preventDefault();
      all[(move + all.length) % all.length]?.focus();
    } else if (event.key === 'Tab') {
      close.current();
    }
  };
  // Kept inside the window by its real width (a long item makes it wider than the 216 assumed below), before it is
  // drawn: at a page's trailing edge (the More button) it ran off the screen.
  useLayoutEffect(() => {
    const element = menu.current;
    if (!element) return;
    // offsetLeft/offsetWidth: the layout box, not scaled by the opening animation.
    const room = document.documentElement.clientWidth - 8;
    if (element.offsetLeft + element.offsetWidth > room) {
      element.style.left = `${Math.max(8, room - element.offsetWidth)}px`;
    }
  }, [rect]);
  const below = rect.bottom + 8 + 48 * items.length < window.innerHeight - 96;
  const left = Math.min(rect.left + 8, document.documentElement.clientWidth - 216);
  return createPortal(
    <div
      ref={menu}
      className={cx('pw-popover pw-menu pw-row-menu', !below && 'is-above')}
      role="menu"
      aria-label={label}
      onKeyDown={onKeyDown}
      style={{
        position: 'fixed',
        left: Math.max(8, left),
        ...(below ? { top: rect.bottom + 8 } : { top: 'auto', bottom: window.innerHeight - rect.top + 8 }),
      }}
    >
      {items.map((item) => (
        <button
          key={item.label}
          type="button"
          role="menuitem"
          className={cx('pw-menu-item', item.destructive && 'is-destructive')}
          onClick={() => {
            close.current();
            item.onSelect();
          }}
        >
          <span className="pw-menu-check" aria-hidden="true">
            {item.icon}
          </span>
          {item.label}
        </button>
      ))}
    </div>,
    document.body,
  );
}
