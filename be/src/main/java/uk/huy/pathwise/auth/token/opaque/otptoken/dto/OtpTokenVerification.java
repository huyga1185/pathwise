package uk.huy.pathwise.auth.token.opaque.otptoken.dto;

/**
 * Outcome of {@code OtpTokenManager.verifyOtpToken} (P-24). Either way the token is consumed.
 */
public sealed interface OtpTokenVerification {
    /**
     * @param userId the user the token was issued for; callers bind it to the user they act on
     */
    record Verified(long userId) implements OtpTokenVerification {}

    record Rejected(RejectReason reason) implements OtpTokenVerification {}

    enum RejectReason {
        /** No token under this ticket and purpose: unknown, expired or already used (Redis cannot tell them apart). */
        NOT_FOUND,
        /** The ticket exists but the secret does not match. */
        MISMATCH
    }
}
