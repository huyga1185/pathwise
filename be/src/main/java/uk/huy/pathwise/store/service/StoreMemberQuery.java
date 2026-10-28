package uk.huy.pathwise.store.service;

import uk.huy.pathwise.store.dto.StoreMember;

import java.util.Collection;
import java.util.List;
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

    /**
     * For showing members already referenced elsewhere (for example the driver of a route), so a member disabled
     * since then is still returned.
     *
     * @return the members among {@code userIds} that belong to {@code storeId}, whatever their status; ids of other
     * stores' users are left out
     */
    List<StoreMember> findMembers(long storeId, Collection<Long> userIds);
}
