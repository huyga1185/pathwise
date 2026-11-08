import type { ReactNode } from 'react';
import { statusInfo, type StatusKind, type Tone } from '@/shared/status';
import { cx } from './cx';

export interface StatusPillProps {
  kind?: StatusKind;
  status?: string | null;
  tone?: Tone;
  children?: ReactNode;
  className?: string;
}

/** Design system StatusPill: label and tone from the API status; nothing for a status without a pill. */
export function StatusPill({ kind, status, tone, children, className }: StatusPillProps) {
  const info = kind ? statusInfo(kind, status) : null;
  const label = children ?? info?.label;
  if (!label) return null;
  return <span className={cx('pw-pill', `pw-pill-${tone ?? info?.tone ?? 'muted'}`, className)}>{label}</span>;
}
