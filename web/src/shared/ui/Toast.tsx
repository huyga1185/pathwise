import type { ReactNode } from 'react';
import { cx } from './cx';

export interface ToastProps {
  tone?: 'info' | 'success' | 'danger';
  children: ReactNode;
  actionLabel?: string;
  onAction?: () => void;
  onClose?: () => void;
  className?: string;
}

/** Design system Toast: short message after an action; errors stay until closed. */
export function Toast({ tone = 'info', children, actionLabel, onAction, onClose, className }: ToastProps) {
  return (
    <div className={cx('pw-toast', `pw-toast-${tone}`, className)} role={tone === 'danger' ? 'alert' : 'status'}>
      <span className="pw-toast-dot" aria-hidden="true" />
      <p className="pw-toast-msg">{children}</p>
      {actionLabel ? (
        <button type="button" className="pw-toast-action" onClick={onAction}>
          {actionLabel}
        </button>
      ) : null}
      {onClose ? (
        <button type="button" className="pw-toast-close" onClick={onClose} aria-label="Đóng thông báo">
          ×
        </button>
      ) : null}
    </div>
  );
}
