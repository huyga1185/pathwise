package uk.huy.pathwise.shared.store;

import org.jspecify.annotations.Nullable;

/**
 * Whether a store can be written to (T56, P-62), as other modules see it.
 *
 * @param lockReason why it is READ_ONLY; null while ACTIVE
 */
public record StoreState(StoreStatus status, @Nullable String lockReason) {}
