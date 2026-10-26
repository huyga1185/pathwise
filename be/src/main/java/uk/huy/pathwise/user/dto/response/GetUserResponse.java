package uk.huy.pathwise.user.dto.response;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;

/**
 * @param canDrive true if the user can be assigned routes: always for DRIVER, otherwise the stored flag (D-21)
 */
public record GetUserResponse(long id,
                              String email,
                              String fullName,
                              String phoneNumber,
                              UserRole role,
                              Long storeId,
                              UserStatus status,
                              boolean canDrive) {}
