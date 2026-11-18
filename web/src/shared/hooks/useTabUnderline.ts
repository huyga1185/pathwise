import { useLayoutEffect, type RefObject } from 'react';

/** Where each remembered underline last stood, so tabs that are separate pages can slide from the previous tab. */
const lastPlace = new Map<string, { width: number; left: number }>();

/**
 * A tab view's underline (HIG Tab views, wide windows): one line under the current tab that slides and resizes to the
 * next one instead of jumping. Placed directly on the element (no re-render); follows resizing. With `remember`, a
 * newly mounted bar (tabs that are separate pages) starts where the previous page's underline stood.
 */
export function useTabUnderline(
  bar: RefObject<HTMLElement | null>,
  line: RefObject<HTMLElement | null>,
  current: string,
  selector = '[aria-selected="true"], [aria-current="page"]',
  remember?: string,
) {
  useLayoutEffect(() => {
    const set = (element: HTMLElement, width: number, left: number) => {
      element.style.setProperty('width', `${width}px`);
      element.style.setProperty('transform', `translateX(${left}px)`);
    };
    const place = () => {
      const tab = bar.current?.querySelector<HTMLElement>(selector);
      const element = line.current;
      if (!tab || !element) return;
      set(element, tab.offsetWidth, tab.offsetLeft);
      if (remember) lastPlace.set(remember, { width: tab.offsetWidth, left: tab.offsetLeft });
    };
    const previous = remember ? lastPlace.get(remember) : undefined;
    const element = line.current;
    if (previous && element && !element.style.width) {
      // Start at the old place without animating, then slide to the new one.
      element.style.setProperty('transition', 'none');
      set(element, previous.width, previous.left);
      void element.offsetWidth;
      element.style.removeProperty('transition');
    }
    place();
    window.addEventListener('resize', place);
    return () => window.removeEventListener('resize', place);
  }, [bar, line, current, selector, remember]);
}
