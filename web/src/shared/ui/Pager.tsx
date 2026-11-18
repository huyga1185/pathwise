import { useEffect, useRef, useState } from 'react';
import { vi } from '@/shared/i18n/vi';
import { cx } from './cx';
import { PageJump } from './Filters';
import { ChevronLeftIcon, ChevronRightIcon } from './icons';

/**
 * Pages of a list with a clear end: "‹ Trang 4/12 ›", centred under the list on a wide window (HIG Page controls:
 * "Center a page control at the bottom of the view"); on a phone above it, pinned (`pinned`, §8.17). The arrows go one page back or on, round grey buttons (HIG Buttons), dimmed with
 * aria-disabled at either end so a focused arrow keeps its focus. "Trang 4/12" opens a small panel: first page, a
 * field for the page number, last page (PageJump). One page: no pager.
 */
export function Pager({
  label,
  page,
  totalPages,
  onPage,
  pinned = false,
}: {
  /** The navigation's accessible name ("Trang danh sách kế hoạch"). */
  label: string;
  page: number;
  totalPages: number;
  onPage: (page: number) => void;
  /**
   * A phone: above the list, pinned under the top bar while the list scrolls under it (owner, 2026-11-18, §8.17; HIG
   * Toolbars, iOS: buttons "above scrolling content… always remain visible, as the content scrolls under them").
   */
  pinned?: boolean;
}) {
  const { sentinel, stuck } = useStuck(pinned);
  if (totalPages <= 1) return null;
  const current = Math.min(page, totalPages - 1);
  const last = totalPages - 1;
  const bar = (
    <nav className="pw-pager" aria-label={label}>
      <button
        type="button"
        className="pw-pager-step"
        aria-label={vi.pager.previous}
        title={vi.pager.previous}
        aria-disabled={current === 0}
        onClick={() => current > 0 && onPage(current - 1)}
      >
        <ChevronLeftIcon />
      </button>
      <PageJump
        page={current}
        pages={totalPages}
        buttonText={vi.pager.pageOf(current + 1, totalPages)}
        buttonLabel={vi.pager.pageJump(current + 1, totalPages)}
        fieldLabel={vi.pager.pageField}
        firstLabel={vi.pager.firstPage}
        lastLabel={vi.pager.lastPage}
        above={!pinned}
        onPage={onPage}
      />
      <button
        type="button"
        className="pw-pager-step"
        aria-label={vi.pager.next}
        title={vi.pager.next}
        aria-disabled={current === last}
        onClick={() => current < last && onPage(current + 1)}
      >
        <ChevronRightIcon />
      </button>
    </nav>
  );
  if (!pinned) return bar;
  return (
    <>
      {/* Where the bar would sit unpinned: once it has scrolled past the top bar, the bar is stuck. */}
      <div ref={sentinel} className="pw-pager-sentinel" aria-hidden="true" />
      <div className={cx('pw-pager-bar', stuck && 'is-stuck')}>{bar}</div>
    </>
  );
}

/**
 * Whether a pinned bar is stuck under the top bar: its sentinel has scrolled up past the top bar's bottom. Stuck, the
 * bar shows its scroll edge (HIG Scroll edge effects: a separation "when a scroll view is behind floating interface
 * elements"); at rest it is part of the page.
 */
function useStuck(enabled: boolean) {
  const sentinel = useRef<HTMLDivElement>(null);
  const [on, setOn] = useState(false);
  useEffect(() => {
    const element = sentinel.current;
    if (!enabled || !element || typeof IntersectionObserver === 'undefined') return undefined;
    const top = document.querySelector('.pw-topbar')?.getBoundingClientRect().height ?? 0;
    const observer = new IntersectionObserver(
      ([entry]) => setOn(Boolean(entry && !entry.isIntersecting && entry.boundingClientRect.top < top + 1)),
      { rootMargin: `-${Math.round(top)}px 0px 0px 0px` },
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, [enabled]);
  return { sentinel, stuck: on };
}
