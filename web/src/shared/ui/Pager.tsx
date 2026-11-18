import { vi } from '@/shared/i18n/vi';
import { PageJump } from './Filters';
import { ChevronLeftIcon, ChevronRightIcon } from './icons';

/**
 * Pages of a list with a clear end: "‹ Trang 4/12 ›", centred under the list (HIG Page controls: "Center a page control
 * at the bottom of the view"). The arrows go one page back or on, round grey buttons (HIG Buttons), dimmed with
 * aria-disabled at either end so a focused arrow keeps its focus. "Trang 4/12" opens a small panel: first page, a
 * field for the page number, last page (PageJump). One page: no pager.
 */
export function Pager({
  label,
  page,
  totalPages,
  onPage,
}: {
  /** The navigation's accessible name ("Trang danh sách kế hoạch"). */
  label: string;
  page: number;
  totalPages: number;
  onPage: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  const current = Math.min(page, totalPages - 1);
  const last = totalPages - 1;
  return (
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
}
