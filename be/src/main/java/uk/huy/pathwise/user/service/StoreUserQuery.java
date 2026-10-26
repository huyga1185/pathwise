package uk.huy.pathwise.user.service;

import uk.huy.pathwise.user.dto.StoreUser;

import java.util.Optional;

/**
 * Users seen as store members, for the {@code store} module.
 */
public interface StoreUserQuery {
    /**
     * @return the user if it belongs to {@code storeId}; empty for another store's user (tenant isolation)
     */
    Optional<StoreUser> findInStore(long storeId, long userId);
}
