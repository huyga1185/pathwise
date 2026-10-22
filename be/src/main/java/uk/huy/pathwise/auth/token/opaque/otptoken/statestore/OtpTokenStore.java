package uk.huy.pathwise.auth.token.opaque.otptoken.statestore;

import uk.huy.pathwise.core.statestore.StateStore;

/**
 * A specialized state store for managing One-Time Password (OTP) tokens.
 * <p>
 * This interface extends {@link StateStore} to provide specific behaviors required
 * for secure OTP verification, primarily ensuring that tokens are strictly "one-time" use.
 * </p>
 */
public interface OtpTokenStore extends StateStore<String, String> {
    /**
     * Atomically retrieves and deletes the value associated with the given key.
     * <p>
     * <b>Security Note:</b> It is crucial that the implementation of this method is
     * strictly atomic at the data store level (e.g., using Redis {@code GETDEL} or
     * database row-level locking). This guarantees that an OTP can only be fetched
     * exactly once, preventing race conditions and replay attacks in scenarios where
     * concurrent verification requests are made.
     * </p>
     *
     * @param key the key whose associated OTP token is to be retrieved and removed
     * @return the hashed OTP token string if the key exists and has not expired,
     *         or {@code null} if the key does not exist
     */
    String getAndDelete(String key);
}
