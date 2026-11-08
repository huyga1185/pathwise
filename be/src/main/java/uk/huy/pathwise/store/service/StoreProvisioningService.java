package uk.huy.pathwise.store.service;

/**
 * Public API of the {@code store} module for registration (D-15). Joins the caller's transaction, so the store and
 * its OWNER are created together.
 */
public interface StoreProvisioningService {
    /**
     * Creates a store with the default timezone and no depot.
     *
     * @return id of the new store
     */
    long createStore(String name);
}
