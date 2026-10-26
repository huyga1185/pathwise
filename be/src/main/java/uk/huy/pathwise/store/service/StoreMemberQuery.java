package uk.huy.pathwise.store.service;

import uk.huy.pathwise.store.dto.StoreMember;

import java.util.Optional;

/**
 * Public API of the {@code store} module for checking members (for example the drivers of a plan) without
 * reading the {@code user} module's data directly.
 */
public interface StoreMemberQuery {
    /**
     * @return the member if {@code userId} belongs to {@code storeId} and is {@code ACTIVE}; empty otherwise,
     * including for a user of another store. Whether the member may drive (D-21) is left to the caller.
     */
    Optional<StoreMember> findActiveMember(long storeId, long userId);
}
