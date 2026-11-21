import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type KeyboardEvent as ReactKeyboardEvent,
  type MouseEvent as ReactMouseEvent,
  type Ref,
} from 'react';
import { useExit } from '@/shared/hooks/useExit';
import { Panel } from './Filters';
import { CheckIcon } from './icons';

interface Choice {
  value: string;
  label: string;
}

/** Hands the element to the caller's ref too (react-hook-form's register gives one). */
function assignRef<T>(ref: Ref<T> | undefined, value: T | null) {
  if (typeof ref === 'function') ref(value);
  else if (ref) (ref as { current: T | null }).current = value;
}

const OPEN_KEYS = new Set([' ', 'Enter', 'ArrowDown', 'ArrowUp']);

/**
 * Turns a native <select> into a pop-up button (HIG Pop-up buttons): pressing it opens the app's menu, the current
 * choice with a checkmark (HIG Menus), instead of the browser's own list. The <select> stays the form control (label,
 * value, react-hook-form, change events), so only how it opens changes. The choices are read from its options when it
 * opens; an empty-valued placeholder is not a choice. Usage: spread `selectProps` on the <select> (merging `ref`), render
 * `menu` after it.
 */
export function useSelectMenu(label: string, outerRef?: Ref<HTMLSelectElement>) {
  const select = useRef<HTMLSelectElement | null>(null);
  const panel = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);
  const presence = useExit(open, 140);
  const [choices, setChoices] = useState<Choice[]>([]);
  const [value, setValue] = useState('');

  const ref = useCallback(
    (element: HTMLSelectElement | null) => {
      select.current = element;
      assignRef(outerRef, element);
    },
    [outerRef],
  );

  const show = () => {
    const element = select.current;
    if (!element || element.disabled) return;
    setChoices(
      [...element.options]
        .filter((option) => option.value !== '' && !option.disabled)
        .map((option) => ({ value: option.value, label: option.text })),
    );
    setValue(element.value);
    setOpen(true);
  };
  const close = (focusSelect: boolean) => {
    setOpen(false);
    if (focusSelect) select.current?.focus();
  };
  const pick = (next: string) => {
    const element = select.current;
    close(true);
    if (!element || element.value === next) return;
    element.value = next;
    element.dispatchEvent(new Event('change', { bubbles: true }));
  };

  useEffect(() => {
    if (!open) return undefined;
    const target =
      panel.current?.querySelector<HTMLElement>('[aria-checked="true"]') ??
      panel.current?.querySelector<HTMLElement>('[role="menuitemradio"]');
    target?.focus();
    target?.scrollIntoView?.({ block: 'nearest' });
    const outside = (event: PointerEvent) => {
      const node = event.target as Node;
      if (!panel.current?.contains(node) && !select.current?.contains(node)) setOpen(false);
    };
    document.addEventListener('pointerdown', outside);
    return () => document.removeEventListener('pointerdown', outside);
  }, [open]);

  const onMenuKeyDown = (event: ReactKeyboardEvent<HTMLDivElement>) => {
    const items = [...(panel.current?.querySelectorAll<HTMLElement>('[role="menuitemradio"]') ?? [])];
    const index = items.indexOf(document.activeElement as HTMLElement);
    const move = { ArrowDown: index + 1, ArrowUp: index - 1, Home: 0, End: items.length - 1 }[event.key];
    if (event.key === 'Escape') {
      event.preventDefault();
      event.stopPropagation();
      close(true);
    } else if (move !== undefined) {
      event.preventDefault();
      items[(move + items.length) % items.length]?.focus();
    } else if (event.key === 'Tab') {
      close(false);
    }
  };

  const selectProps = {
    ref,
    'aria-haspopup': 'menu' as const,
    'aria-expanded': open,
    onMouseDown: (event: ReactMouseEvent<HTMLSelectElement>) => {
      if (event.button !== 0) return;
      event.preventDefault();
      event.currentTarget.focus();
      if (open) close(false);
      else show();
    },
    onKeyDown: (event: ReactKeyboardEvent<HTMLSelectElement>) => {
      if (!OPEN_KEYS.has(event.key) || event.altKey || event.metaKey || event.ctrlKey) return;
      event.preventDefault();
      show();
    },
  };

  const menu = (
    <Panel
      presence={presence}
      anchorRef={select}
      panelRef={panel}
      className="pw-menu pw-select-menu"
      matchWidth
      role="menu"
      aria-label={label}
      onKeyDown={onMenuKeyDown}
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
            onClick={() => pick(choice.value)}
          >
            <span className="pw-menu-check" aria-hidden="true">
              {checked ? <CheckIcon /> : null}
            </span>
            {choice.label}
          </button>
        );
      })}
    </Panel>
  );

  return { selectProps, menu };
}
