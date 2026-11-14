package uk.huy.pathwise.auth.resetlink;

import java.time.Duration;
import java.util.Optional;

/**
 * Password reset links sent by an ADMIN (T57, P-63): at most one per user, by the hash of its token.
 */
public interface ResetLinkStore {
    /**
     * Saves the link of the user, replacing the previous one.
     */
    void save(long userId, String tokenHash, Duration ttl);

    /**
     * @return the user of the link, if it exists and has not expired
     */
    Optional<Long> findUserId(String tokenHash);

    /**
     * Deletes the link (used); a newer link of the same user stays.
     */
    void delete(long userId, String tokenHash);
}
