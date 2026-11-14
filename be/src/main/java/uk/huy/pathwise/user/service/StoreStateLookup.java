package uk.huy.pathwise.user.service;

import uk.huy.pathwise.shared.store.StoreState;

import java.util.Optional;

/**
 * State of a store as {@code GET /users/me} shows it (T56, P-62). Declared here and implemented by the {@code store}
 * module, so that {@code user} does not depend on {@code store} (ARCHITECTURE.md §2.1).
 */
public interface StoreStateLookup {
    /**
     * @return the state, or empty if there is no such store
     */
    Optional<StoreState> findState(long storeId);
}
