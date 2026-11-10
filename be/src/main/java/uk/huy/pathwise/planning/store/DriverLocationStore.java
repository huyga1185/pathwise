package uk.huy.pathwise.planning.store;

import uk.huy.pathwise.planning.model.LocationPoint;

import java.time.Duration;
import java.util.Optional;

/**
 * Last known position of each driver (P-15), kept for a short time. Fails with
 * {@link uk.huy.pathwise.core.statestore.StateStoreException} when the store cannot be reached.
 */
public interface DriverLocationStore {
    /**
     * Stores {@code point} unless the stored one was recorded at the same time or later, in one atomic step.
     *
     * @param ttl how long the point is kept
     * @return whether {@code point} was stored
     */
    boolean saveIfNewer(long storeId, long driverId, LocationPoint point, Duration ttl);

    Optional<LocationPoint> find(long storeId, long driverId);
}
