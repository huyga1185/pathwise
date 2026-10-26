package uk.huy.pathwise.user.dto;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;

/**
 * A user seen as a member of their store.
 *
 * @param canDrive the stored flag; a DRIVER can always be assigned routes whatever its value (D-21)
 */
public record StoreUser(long id, String email, String fullName, UserRole role, UserStatus status, boolean canDrive) {}
