package uk.huy.pathwise.store.service;

import uk.huy.pathwise.store.dto.StoreProfile;

import java.util.Optional;

/**
 * Public API of the {@code store} module for reading store settings, for example the default depot and timezone that
 * a plan snapshots (D-20, P-14).
 */
public interface StoreQuery {
    /**
     * @return the store, or empty if it does not exist
     */
    Optional<StoreProfile> findStore(long storeId);
}
