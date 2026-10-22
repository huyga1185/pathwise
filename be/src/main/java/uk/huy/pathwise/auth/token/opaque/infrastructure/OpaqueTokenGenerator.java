package uk.huy.pathwise.auth.token.opaque.infrastructure;

/**
 * A generator interface for creating opaque tokens.
 * <p>
 * An opaque token is a random, structureless string that holds no intrinsic
 * meaning or readable information for the client (unlike structured tokens
 * such as JWTs). It serves purely as a secure reference identifier to state
 * stored on the server side (e.g., session data, refresh tokens, or OTP records).
 * </p>
 */
public interface OpaqueTokenGenerator {
    /**
     * Generates a new opaque token.
     * <p>
     * <b>Security Note:</b> Implementations of this method must ensure that the
     * generated tokens are highly unpredictable, cryptographically secure, and
     * possess sufficient entropy to prevent guessing or brute-force attacks.
     * Avoid using predictable generation logic (like standard UUIDv4 alone if
     * high security is required, prefer {@link java.security.SecureRandom}).
     * </p>
     *
     * @return a secure, unique, and non-readable string representing the opaque token
     */
    String generateOpaqueToken();
}
