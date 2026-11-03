import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react';
import { browserPreferences, type PreferenceStore } from '@/platform/preferences';

export type ThemePreference = 'system' | 'light' | 'dark';
export type Theme = 'light' | 'dark';

interface ThemeValue {
  preference: ThemePreference;
  theme: Theme;
  setPreference(preference: ThemePreference): void;
}

const STORAGE_KEY = 'pw-theme';
const ThemeContext = createContext<ThemeValue | null>(null);

function systemTheme(): Theme {
  return typeof window !== 'undefined' && window.matchMedia?.('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light';
}

/**
 * Light/dark theme (DESIGN_SYSTEM "Theme sáng và tối"): follows the system until the user picks one, and remembers
 * the pick on this device. Sets `data-theme` on <html>, which tokens.css reads.
 */
export function ThemeProvider({
  children,
  preferences = browserPreferences,
}: {
  children: ReactNode;
  preferences?: PreferenceStore;
}) {
  const [preference, setPreferenceState] = useState<ThemePreference>(() => {
    const saved = preferences.read(STORAGE_KEY);
    return saved === 'light' || saved === 'dark' ? saved : 'system';
  });
  const [system, setSystem] = useState<Theme>(systemTheme);

  useEffect(() => {
    const media = window.matchMedia?.('(prefers-color-scheme: dark)');
    if (!media) return undefined;
    const onChange = () => setSystem(media.matches ? 'dark' : 'light');
    media.addEventListener('change', onChange);
    return () => media.removeEventListener('change', onChange);
  }, []);

  const theme: Theme = preference === 'system' ? system : preference;
  useEffect(() => {
    document.documentElement.dataset.theme = theme;
  }, [theme]);

  const setPreference = useCallback(
    (next: ThemePreference) => {
      setPreferenceState(next);
      preferences.write(STORAGE_KEY, next === 'system' ? null : next);
    },
    [preferences],
  );
  const value = useMemo(() => ({ preference, theme, setPreference }), [preference, theme, setPreference]);
  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme(): ThemeValue {
  const value = useContext(ThemeContext);
  if (!value) throw new Error('useTheme must be used inside ThemeProvider');
  return value;
}
