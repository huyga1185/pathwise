/**
 * Small per-device preferences (theme). Storage may be unavailable (private mode, blocked site data), so every access
 * is guarded and a missing value is not an error.
 */
export interface PreferenceStore {
  read(key: string): string | null;
  write(key: string, value: string | null): void;
}

export const browserPreferences: PreferenceStore = {
  read(key) {
    try {
      return window.localStorage.getItem(key);
    } catch {
      return null;
    }
  },
  write(key, value) {
    try {
      if (value === null) window.localStorage.removeItem(key);
      else window.localStorage.setItem(key, value);
    } catch {
      // Not remembering a preference is acceptable.
    }
  },
};
