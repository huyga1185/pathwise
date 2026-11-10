package uk.huy.pathwise.planning.store;

import uk.huy.pathwise.planning.model.LocationSharing;

import java.time.Duration;
import java.util.Optional;

/**
 * The sharing state each driver's app last reported (P-53): one value per driver, for the route it names. Fails with
 * {@link uk.huy.pathwise.core.statestore.StateStoreException} when the store cannot be reached.
 */
public interface LocationSharingStore {
    void save(long storeId, long driverId, LocationSharing sharing, Duration ttl);

    Optional<LocationSharing> find(long storeId, long driverId);
}
