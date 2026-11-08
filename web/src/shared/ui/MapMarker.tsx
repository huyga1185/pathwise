import { cx } from './cx';

export interface MapMarkerProps {
  sequence?: number;
  tone?: 'route' | 'signal' | 'success' | 'danger' | 'muted';
  /** 0-based route position; colours map-route-(n % 7 + 1) and overrides tone. */
  routeIndex?: number | null;
  selected?: boolean;
  label?: string;
  className?: string;
}

/** Design system MapMarker. Always light-theme colours, map tiles are light (data-theme="light"). */
export function MapMarker({ sequence, tone = 'route', routeIndex, selected, label, className }: MapMarkerProps) {
  const name = label ?? (sequence !== undefined ? `Điểm ${sequence}` : 'Điểm giao');
  const colour =
    routeIndex !== undefined && routeIndex !== null ? `pw-marker-r${(routeIndex % 7) + 1}` : `pw-marker-${tone}`;
  return (
    <span
      data-theme="light"
      role="img"
      aria-label={name}
      className={cx('pw-marker', colour, selected && 'pw-marker-selected', className)}
    >
      {sequence ?? ''}
    </span>
  );
}
