package uk.huy.pathwise.admin.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.shared.store.StoreStatus;

import java.time.Instant;

/**
 * State of a store after a lock or an unlock (T56).
 *
 * @param lockReason null while ACTIVE
 * @param lockedAt   null while ACTIVE
 */
public record AdminStoreStateResponse(long id, StoreStatus status, @Nullable String lockReason, @Nullable Instant lockedAt) {}
