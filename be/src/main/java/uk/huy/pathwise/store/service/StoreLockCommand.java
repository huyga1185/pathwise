package uk.huy.pathwise.store.service;

import uk.huy.pathwise.store.dto.StoreRecord;

/**
 * Locks and unlocks a store, for the {@code admin} module (T56, P-59). Call it inside the caller's transaction.
 */
public interface StoreLockCommand {
    /**
     * Makes the store READ_ONLY with this reason; locking a locked store replaces the reason and the locker.
     *
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND} if there is no such store
     */
    StoreRecord lock(long storeId, long adminId, String reason);

    /**
     * Makes the store ACTIVE again; an ACTIVE store is left as it is.
     *
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND} if there is no such store
     */
    StoreRecord unlock(long storeId);
}
