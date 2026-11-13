package uk.huy.pathwise.core.housekeeping;

import java.time.Duration;

/**
 * Lets one application instance at a time run the housekeeping (several instances share the database).
 */
public interface HousekeepingLock {
    /**
     * @return a token to release the lock with, or null if another instance holds it
     */
    String tryAcquire(Duration ttl);

    /**
     * Releases the lock if {@code token} still holds it; a lock that expired and was taken by another run is left.
     */
    void release(String token);
}
