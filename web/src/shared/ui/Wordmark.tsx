import { cx } from './cx';

/**
 * The PathWise wordmark: "Path" in the text colour, "Wise" in the brand accent (chủ dự án, 2026-11-16) — the route
 * teal on light surfaces (it lightens in the dark theme), the mint of the account screens' headline on the brand
 * gradient or a dark picture (onBrand). Read as one word.
 */
export function Wordmark({ onBrand = false }: { onBrand?: boolean }) {
  return (
    <span className={cx('pw-wordmark', onBrand && 'pw-wordmark-on-brand')}>
      Path<span className="pw-wordmark-accent">Wise</span>
    </span>
  );
}
