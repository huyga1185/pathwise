package uk.huy.pathwise.user.dto;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;

/**
 * A user seen as a member of their store.
 *
 * @param phoneNumber E.164 (e.g. {@code +84905123456}), or null
 * @param canDrive    the stored flag; a DRIVER can always be assigned routes whatever its value (D-21)
 */
public record StoreUser(long id,
                        String email,
                        String fullName,
                        String phoneNumber,
                        UserRole role,
                        UserStatus status,
                        boolean canDrive) {}
