export interface ProgressBarProps {
  label: string;
  /** Leave value/max out for an indeterminate bar. */
  value?: number;
  max?: number;
  className?: string;
}

/** Design system ProgressBar: background work of a plan (geocode, optimize). */
export function ProgressBar({ label, value, max, className }: ProgressBarProps) {
  const known = typeof value === 'number' && typeof max === 'number' && max > 0;
  const percent = known ? Math.max(0, Math.min(100, (value / max) * 100)) : 0;
  return (
    <div className={['pw-progress', className].filter(Boolean).join(' ')}>
      <div className="pw-progress-head">
        <span className="pw-progress-label">{label}</span>
        {known ? <span className="pw-progress-count">{`${value}/${max}`}</span> : null}
      </div>
      <div
        className={known ? 'pw-progress-track' : 'pw-progress-track pw-progress-indeterminate'}
        role="progressbar"
        aria-label={label}
        aria-valuemin={known ? 0 : undefined}
        aria-valuemax={known ? max : undefined}
        aria-valuenow={known ? value : undefined}
      >
        <div className="pw-progress-fill" style={known ? { width: `${percent}%` } : undefined} />
      </div>
    </div>
  );
}
