import { fieldMessage } from '@/shared/errors';

/**
 * Instant checks that repeat the server's own (ARCHITECTURE §2 rule 5): required, length, format. The server still
 * decides; its field errors replace these.
 */
/** Seconds before another code may be asked for (UX_BRIEF §4.1). */
export const RESEND_COOLDOWN_SECONDS = 60;
/** Wrong codes after which a code is cancelled and a new one is needed (API_SPEC §2). */
export const MAX_OTP_TRIES = 3;

export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** Password of 8 to 72 characters (API_SPEC §2), the messages of UX_BRIEF §6.2. */
export function passwordRules(label: string) {
  return {
    validate: (value: string) =>
      value === ''
        ? fieldMessage('PASSWORD_REQUIRED', label)
        : (value.length >= 8 && value.length <= 72) || fieldMessage('INVALID_PASSWORD_LENGTH', label),
  };
}

/** A required text, trimmed. */
export function requiredRule(code: string, label: string) {
  return { validate: (value: string) => value.trim() !== '' || fieldMessage(code, label) };
}

/** Email: required and of a valid form. */
export function emailRules(label: string) {
  return {
    validate: (value: string) =>
      value.trim() === ''
        ? fieldMessage('EMAIL_REQUIRED', label)
        : EMAIL_PATTERN.test(value.trim()) || fieldMessage('INVALID_EMAIL_FORMAT', label),
  };
}
