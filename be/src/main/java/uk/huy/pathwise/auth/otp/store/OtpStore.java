package uk.huy.pathwise.auth.otp.store;

import uk.huy.pathwise.auth.otp.dto.Otp;

/**
 * A central facade for managing the lifecycle of One-Time Passwords (OTPs).
 * <p>
 * This manager acts as an orchestrator, coordinating underlying components such as
 * token generators, hashers, and state stores. It provides a clean, high-level API
 * for business services to issue and validate OTPs without exposing the underlying
 * cryptographic or persistence details.
 * </p>
 */
public interface OtpStore {
    /**
     * Generates a secure OTP, securely stores its hashed version, and returns the details.
     * <p>
     * The implementation should generate a raw token, hash it for secure storage,
     * and persist it using a composite key derived from the {@code purpose} and
     * {@code identifier}. The returned {@link Otp} object contains the raw token
     * which can then be transmitted to the user (e.g., via Email or SMS).
     * </p>
     *
     * @param purpose    the context or action for which the OTP is requested
     *                   (e.g., "PASSWORD_RESET", "2FA_LOGIN")
     * @param identifier the unique identifier of the subject requesting the OTP
     *                   (e.g., an email address, user ID, or session ticket)
     * @return an {@link Otp} object containing the raw token.
     * @throws uk.huy.pathwise.core.statestore.StateStoreException if the underlying store is unavailable
     */
    Otp generateAndStoreOtp(String purpose, String identifier);
    /**
     * Verifies the provided raw OTP against the stored record for the given purpose and identifier.
     * <p>
     * <b>Security Note:</b> A successful verification must automatically invalidate
     * (consume) the OTP to ensure it cannot be reused. This should be handled
     * atomically by the underlying state store.
     * </p>
     *
     * @param rawOtp     the unhashed, plain-text OTP provided by the user
     * @param purpose    the context or action for which the OTP was originally issued
     * @param identifier the unique identifier associated with the OTP
     * @return {@code true} if the OTP is valid and matches the stored hash,
     *         {@code false} if it is invalid, missing, or expired
     * @throws uk.huy.pathwise.core.statestore.StateStoreException if the underlying store is unavailable
     */
    boolean verifyOtp(String rawOtp, String purpose, String identifier);
}
