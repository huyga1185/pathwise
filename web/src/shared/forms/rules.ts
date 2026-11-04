import { fieldMessage } from '@/shared/errors';

/**
 * Instant checks of a form that repeat the server's own (ARCHITECTURE §2 rule 5): required, length, format, with the
 * sentences of UX_BRIEF §6.2. The server still decides; its field errors replace these.
 */
export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** A required text, trimmed; `code` is the server's code for it (`VEHICLE_NAME_REQUIRED`). */
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

/** Password of 8 to 72 characters (API_SPEC §2). */
export function passwordRules(label: string) {
  return {
    validate: (value: string) =>
      value === ''
        ? fieldMessage('PASSWORD_REQUIRED', label)
        : (value.length >= 8 && value.length <= 72) || fieldMessage('INVALID_PASSWORD_LENGTH', label),
  };
}
