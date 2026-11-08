/** Rules of the account screens; the general form checks live in shared/forms/rules. */
export { EMAIL_PATTERN, emailRules, passwordRules, requiredRule } from '@/shared/forms/rules';

/** Seconds before another code may be asked for (UX_BRIEF §4.1). */
export const RESEND_COOLDOWN_SECONDS = 60;
/** Wrong codes after which a code is cancelled and a new one is needed (API_SPEC §2). */
export const MAX_OTP_TRIES = 3;
