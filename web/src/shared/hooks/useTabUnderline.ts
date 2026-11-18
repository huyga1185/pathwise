import { useLayoutEffect, type RefObject } from 'react';

/**
 * A tab view's underline (HIG Tab views, wide windows): one line under the current tab that slides and resizes to the
 * next one instead of jumping. Placed directly on the element (no re-render); follows resizing.
 */
export function useTabUnderline(
  bar: RefObject<HTMLElement | null>,
  line: RefObject<HTMLElement | null>,
  current: string,
  selector = '[aria-selected="true"], [aria-current="page"]',
) {
  useLayoutEffect(() => {
    const place = () => {
      const tab = bar.current?.querySelector<HTMLElement>(selector);
      const element = line.current;
      if (!tab || !element) return;
      element.style.setProperty('width', `${tab.offsetWidth}px`);
      element.style.setProperty('transform', `translateX(${tab.offsetLeft}px)`);
    };
    place();
    window.addEventListener('resize', place);
    return () => window.removeEventListener('resize', place);
  }, [bar, line, current, selector]);
}
