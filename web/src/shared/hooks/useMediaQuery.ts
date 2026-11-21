import { useEffect, useState } from 'react';

/** Whether a media query matches now, following changes (window resized, device rotated). */
export function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() => window.matchMedia?.(query).matches ?? false);
  useEffect(() => {
    const media = window.matchMedia?.(query);
    if (!media) return undefined;
    const update = () => setMatches(media.matches);
    update();
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, [query]);
  return matches;
}

/** The app's wide layout (sidebar, tables) from 960px; narrower is the phone/tablet layout (tab bar, lists). */
export const WIDE_QUERY = '(min-width: 960px)';
