package uk.huy.pathwise.auth.service;

/**
 * Public API of {@code auth} for the {@code admin} module (T57, FR-A13): emails an ADMIN can send for a user. The
 * ADMIN never sees a code, a link or a password.
 */
public interface AccountSupport {
    /**
     * Emails a new verification code to an account waiting for verification, like "Gửi lại mã".
     *
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND}; {@code EMAIL_ALREADY_VERIFIED} if the
     *         email is verified; {@code ACCOUNT_DISABLED}; {@code RATE_LIMITED}, {@code OTP_COOLDOWN}
     */
    void sendVerificationEmail(long userId);

    /**
     * Emails a password reset link (P-63): it opens a form for 24 hours, and changing the password there still needs
     * a code sent to the same email. A new link replaces the previous one.
     *
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND}; {@code ACCOUNT_DISABLED};
     *         {@code EMAIL_NOT_VERIFIED} for an account waiting for verification
     */
    void sendPasswordResetLink(long userId);
}
