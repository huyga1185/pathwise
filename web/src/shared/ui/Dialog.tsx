import { useEffect, useId, useRef, type ReactNode } from 'react';
import { Button } from './Button';
import { cx } from './cx';

export interface DialogProps {
  open: boolean;
  title: ReactNode;
  children?: ReactNode;
  confirmLabel: ReactNode;
  cancelLabel?: ReactNode;
  tone?: 'primary' | 'danger';
  /** md 440px · lg 720px · full: near full screen (map picker). */
  size?: 'md' | 'lg' | 'full';
  busy?: boolean;
  confirmDisabled?: boolean;
  onConfirm?: () => void;
  onClose?: () => void;
}

/** Design system Dialog: focus on open, Esc closes, focus goes back to where it was. */
export function Dialog({
  open,
  title,
  children,
  confirmLabel,
  cancelLabel = 'Quay lại',
  tone = 'primary',
  size = 'md',
  busy,
  confirmDisabled,
  onConfirm,
  onClose,
}: DialogProps) {
  const titleId = useId();
  const ref = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    if (!open) return undefined;
    const previous = document.activeElement as HTMLElement | null;
    ref.current?.querySelector<HTMLElement>('button, [href], input, textarea, select')?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCloseRef.current?.();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, [open]);

  if (!open) return null;
  return (
    <div className="pw-dialog-backdrop">
      <div
        ref={ref}
        className={cx('pw-dialog', size !== 'md' && `pw-dialog-${size}`)}
        role={tone === 'danger' ? 'alertdialog' : 'dialog'}
        aria-modal="true"
        aria-labelledby={titleId}
      >
        <h2 id={titleId} className="pw-dialog-title">
          {title}
        </h2>
        <div className="pw-dialog-body">{children}</div>
        <div className="pw-dialog-actions">
          <Button variant="secondary" onClick={onClose}>
            {cancelLabel}
          </Button>
          <Button
            variant={tone === 'danger' ? 'danger' : 'primary'}
            onClick={onConfirm}
            busy={busy}
            disabled={confirmDisabled}
          >
            {confirmLabel}
          </Button>
        </div>
      </div>
    </div>
  );
}
