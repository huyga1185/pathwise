import type { ReactNode } from 'react';
import { formatPhone } from '@/shared/format';
import { cx } from './cx';
import { StatusPill } from './StatusPill';

export interface StopCardProps {
  sequence?: number;
  title: ReactNode;
  address: ReactNode;
  eta?: string;
  timeWindow?: string;
  note?: ReactNode;
  status?: 'PENDING' | 'DELIVERED' | 'FAILED' | 'SKIPPED';
  failureReason?: string;
  /** E.164 from the API. */
  phone?: string;
  current?: boolean;
  actions?: ReactNode;
  className?: string;
}

/** Design system StopCard (reference for the driver app; used on the web where a stop is shown as a card). */
export function StopCard({
  sequence,
  title,
  address,
  eta,
  timeWindow,
  note,
  status,
  failureReason,
  phone,
  current,
  actions,
  className,
}: StopCardProps) {
  const shownPhone = phone ? formatPhone(phone) : null;
  const meta = [eta ? `Dự kiến ${eta}` : null, timeWindow ? `Khung ${timeWindow}` : null].filter(Boolean).join(' · ');
  return (
    <article
      className={cx('pw-stop', current && 'pw-stop-current', className)}
      aria-current={current ? 'step' : undefined}
    >
      <div className="pw-stop-head">
        {sequence !== undefined ? (
          <span className="pw-stop-seq" aria-label={`Điểm số ${sequence}`}>
            {sequence}
          </span>
        ) : null}
        <h3 className="pw-stop-title">{title}</h3>
        {status ? <StatusPill kind="stop" status={status} /> : null}
      </div>
      <p className="pw-stop-address">{address}</p>
      {meta ? <p className="pw-stop-meta">{meta}</p> : null}
      {note ? <p className="pw-stop-note">{note}</p> : null}
      {failureReason ? <p className="pw-stop-reason">{`Lý do: ${failureReason}`}</p> : null}
      {shownPhone || actions ? (
        <div className="pw-stop-actions">
          {shownPhone ? (
            <a className="pw-btn pw-btn-secondary pw-btn-lg" href={`tel:${phone}`} aria-label={`Gọi ${shownPhone}`}>
              {`Gọi ${shownPhone}`}
            </a>
          ) : null}
          {actions}
        </div>
      ) : null}
    </article>
  );
}
