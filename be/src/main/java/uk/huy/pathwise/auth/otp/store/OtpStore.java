package uk.huy.pathwise.auth.otp.store;

import uk.huy.pathwise.auth.otp.model.OtpPurpose;

import java.time.Duration;

/**
 * Redis primitives for OTPs, keyed by purpose and user id (ARCHITECTURE.md §6.1, DATA_MODEL.md §3). Only hashes are
 * stored. Business flow (generate, send, release on failure) lives in {@code OtpManager}.
 * Every method throws {@link uk.huy.pathwise.core.statestore.StateStoreException} when Redis fails (fail-closed).
 */
public interface OtpStore {
    /**
     * Starts the cooldown if none is running ({@code SET NX} with TTL).
     *
     * @return false if a cooldown is already running
     */
    boolean tryStartCooldown(OtpPurpose purpose, long userId, Duration cooldown);

    /**
     * Stores the hash of a new OTP with zero attempts, replacing any previous OTP of the same purpose.
     */
    void save(OtpPurpose purpose, long userId, String hashedOtp, Duration ttl);

    /**
     * Checks the hash: a match deletes the OTP (single use); a mismatch counts an attempt and deletes the OTP once
     * {@code maxAttempts} wrong attempts are reached.
     * <p>
     * Implementations must make this check atomic.
     *
     * @return true only if the OTP matched and was consumed
     */
    boolean verify(OtpPurpose purpose, long userId, String hashedOtp, int maxAttempts);

    /**
     * Removes the OTP and its cooldown, e.g. when the email carrying it could not be sent (F-10).
     */
    void discard(OtpPurpose purpose, long userId);
}
