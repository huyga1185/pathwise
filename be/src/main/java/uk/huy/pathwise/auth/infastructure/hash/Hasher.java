package uk.huy.pathwise.auth.infastructure.hash;

/**
 * Defines a general-purpose contract for one-way string hashing and verification
 * of internal system data (e.g., opaque tokens, API keys, sensitive identifiers).
 *
 * <p><b>Architecture Notes:</b>
 * This interface abstracts the underlying cryptographic algorithm away from the core
 * business logic. Implementations can range from fast, unsalted algorithms (e.g., SHA-256)
 * to slower, salted algorithms depending on the sensitivity and entropy of the target data.
 * This decoupling allows the system to easily adapt its hashing strategies without
 * modifying domain services.
 *
 * <p><b>Domain Boundaries:</b>
 * While this interface is highly flexible and supports any algorithm dictated by
 * system requirements, it is strictly designated for non-password data.
 * <b>DO NOT</b> use this interface for user password hashing. For password management,
 * the system relies on Spring Security's standard {@code PasswordEncoder}.
 *
 * <p><b>Implementation Contract:</b>
 * - All implementations MUST be stateless and thread-safe, ensuring they can be
 *   safely injected as Singletons in the Dependency Injection container.
 * - If an implementation requires salting, it is fully responsible for securely
 *   generating, embedding, and extracting the salt internally.
 */
public interface Hasher {
    /**
     * Applies a one-way cryptographic hash function to the given plain text.
     *
     * @param rawString The plain text string to be hashed. Must not be null.
     * @return The securely hashed representation of the input.
     */
    String hashString(String rawString);
    /**
     * Verifies whether a plain text string matches a previously hashed string.
     *
     * <p><b>Security Note:</b> Implementations MUST ensure constant-time
     * string comparison to prevent Timing Attacks.
     *
     * @param hashedString The hashed string retrieved from secure storage.
     * @param rawString    The plain text string submitted for verification.
     * @return {@code true} if the raw string mathematically matches the hash.
     */
    boolean match(String hashedString, String rawString);
}
