package uk.huy.pathwise.store.service;

import uk.huy.pathwise.store.dto.OwnerTransfer;

/**
 * Change of OWNER, for the {@code admin} module (T57, FR-A13, P-63). Call it inside the caller's transaction.
 */
public interface StoreOwnershipCommand {
    /**
     * Makes {@code newOwnerId} the OWNER and the previous OWNER a DISPATCHER (BR-03: one OWNER). A DRIVER who becomes
     * OWNER keeps driving ({@code canDrive = true}). Both are emailed after the commit; a failed email is only logged.
     *
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND} for an unknown store or a user who is not
     *         a member of it; {@code VALIDATION_ERROR} on {@code userId} with {@code NEW_OWNER_NOT_ACTIVE} (not ACTIVE,
     *         so not verified or disabled) or {@code ALREADY_OWNER}
     */
    OwnerTransfer transferOwner(long storeId, long newOwnerId);
}
