package uk.huy.pathwise.auth.otp.model;

/**
 * Why an OTP was issued (ARCHITECTURE.md §6.1). Part of the Redis key, so an OTP issued for one purpose never
 * verifies for another.
 */
public enum OtpPurpose {
    VERIFY_EMAIL, RESET_PASSWORD, CHANGE_PASSWORD
}
