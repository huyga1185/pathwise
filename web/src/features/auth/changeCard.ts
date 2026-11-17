import { flushSync } from 'react-dom';

type WithViewTransition = Document & { startViewTransition?: (update: () => void) => unknown };

/**
 * Changes the content of an account card in place (next step, "Xong") through a same-page View Transition, like the
 * page changes between the cards (login.css): the surface grows or shrinks to the new height and the content fades,
 * instead of jumping. React applies the update inside the transition (flushSync) so the new state is what the
 * browser captures. No View Transitions or reduced motion: the change is applied at once.
 */
export function changeCard(update: () => void) {
  const doc = document as WithViewTransition;
  const still = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if (!doc.startViewTransition || still) {
    update();
    return;
  }
  doc.startViewTransition(() => flushSync(update));
}
