import { createPortal } from 'react-dom';
import { Link } from 'react-router';
import { useTopbarLeading } from './AppShell';
import { ChevronLeftIcon } from './icons';

/**
 * The standard back button (OWNER_UI_REDESIGN G4.1, D6): a chevron and the title of the page it returns to, never a
 * text "Back" (HIG Toolbars: "Use the standard Back and Close buttons… Prefer the standard symbols… don't use a text
 * label that says Back"). A long parent title keeps one line.
 *
 * On a phone it goes to the leading end of the top bar, level with the account button, as a navigation bar's back
 * button (§6, §9.11; HIG Toolbars: "Elements that let people return to the previous document… appear at the far
 * leading edge"). When the small title shows in the middle of the bar, only the chevron stays (as iOS when the
 * previous title has no room); its name is still the parent title.
 */
export function BackLink({ to, label }: { to: string; label: string }) {
  const leading = useTopbarLeading();
  const link = (
    <Link to={to} className="pw-back-link" title={label}>
      <ChevronLeftIcon />
      <span className="pw-back-link-label">{label}</span>
    </Link>
  );
  // Not drawn in the page for a frame before the top bar is there (it jumped up).
  if (leading === 'pending') return null;
  return leading ? createPortal(link, leading) : link;
}
