import { useState } from 'react';
import { EllipsisIcon } from './icons';
import { RowMenu, type RowMenuItem } from './RowMenu';

/**
 * The More menu of a page's top row (a plan's screens, an ADMIN store) (OWNER_UI_REDESIGN G5.6, G5-Q5; HIG Toolbars: "Add a More menu to contain
 * additional actions"): a round button with an ellipsis opening the app's menu under it; a destructive item last, in
 * red. Unavailable items are left out ("Hide unavailable menu items"); with none, no button.
 */
export function MoreMenu({ items, label }: { items: RowMenuItem[]; label: string }) {
  const [rect, setRect] = useState<DOMRect | null>(null);
  if (items.length === 0) return null;
  return (
    <>
      <button
        type="button"
        className="pw-icon-button pw-more-button"
        aria-label={label}
        title={label}
        aria-haspopup="menu"
        aria-expanded={rect !== null}
        onClick={(event) => setRect(event.currentTarget.getBoundingClientRect())}
      >
        <EllipsisIcon />
      </button>
      {rect ? <RowMenu rect={rect} label={label} items={items} onClose={() => setRect(null)} /> : null}
    </>
  );
}
