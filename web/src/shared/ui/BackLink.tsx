import { Link } from 'react-router';
import { ChevronLeftIcon } from './icons';

/**
 * The standard back button (OWNER_UI_REDESIGN G4.1, D6): a chevron and the title of the page it returns to, never a
 * text "Back" (HIG Toolbars: "Use the standard Back and Close buttons… Prefer the standard symbols… don't use a text
 * label that says Back"). A long parent title keeps one line.
 */
export function BackLink({ to, label }: { to: string; label: string }) {
  return (
    <Link to={to} className="pw-back-link" title={label}>
      <ChevronLeftIcon />
      <span>{label}</span>
    </Link>
  );
}
