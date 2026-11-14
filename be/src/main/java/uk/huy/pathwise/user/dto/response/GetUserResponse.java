package uk.huy.pathwise.user.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.shared.store.StoreStatus;

/**
 * @param canDrive        true if the user can be assigned routes: always for DRIVER, otherwise the stored flag (D-21)
 * @param storeStatus     state of the user's store (T56): READ_ONLY shows the lock banner; null for an ADMIN
 * @param storeLockReason why the store is locked; null unless READ_ONLY
 */
public record GetUserResponse(long id,
                              String email,
                              @Nullable String fullName,
                              @Nullable String phoneNumber,
                              UserRole role,
                              @Nullable Long storeId,
                              UserStatus status,
                              boolean canDrive,
                              @Nullable StoreStatus storeStatus,
                              @Nullable String storeLockReason) {}
