import { useEffect, useState } from 'react';

export function reducedMotion(): boolean {
  return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches ?? false;
}

/**
 * Keeps something mounted for `ms` after it is hidden, so it can animate out ("open" → "closing" → "closed"); with
 * reduced motion it goes straight to "closed".
 */
export function useExit(shown: boolean, ms: number): 'open' | 'closing' | 'closed' {
  const [state, setState] = useState<'open' | 'closing' | 'closed'>(shown ? 'open' : 'closed');
  const [wasShown, setWasShown] = useState(shown);
  if (shown !== wasShown) {
    setWasShown(shown);
    setState(shown ? 'open' : reducedMotion() ? 'closed' : 'closing');
  }
  useEffect(() => {
    if (state !== 'closing') return undefined;
    const timer = window.setTimeout(() => setState('closed'), ms);
    return () => window.clearTimeout(timer);
  }, [state, ms]);
  return state;
}
