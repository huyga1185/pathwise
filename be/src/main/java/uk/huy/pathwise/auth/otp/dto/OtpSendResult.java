package uk.huy.pathwise.auth.otp.dto;

/**
 * Outcome of asking for an OTP. Delivery failures are not an outcome but an exception (P-24).
 */
public enum OtpSendResult {
    SENT,
    /** An OTP of the same purpose was sent to this user less than {@code otp.cooldown} ago; nothing was sent. */
    COOLDOWN
}
