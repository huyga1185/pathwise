import { useEffect, useRef, useState } from 'react';

/**
 * Whether a pinned bar is stuck under the top bar: its sentinel has scrolled up past the top bar's bottom. Stuck, the
 * bar shows its scroll edge (HIG Scroll edge effects: a separation "when a scroll view is behind floating interface
 * elements"); at rest it is part of the page.
 */
export function useStuck(enabled: boolean) {
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
