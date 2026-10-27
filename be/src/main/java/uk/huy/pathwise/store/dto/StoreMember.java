package uk.huy.pathwise.store.dto;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;

/**
 * A user of a store, as other modules see it (ARCHITECTURE.md §2.1).
 *
 * @param phoneNumber national format, or null
 * @param canDrive    the stored flag; a DRIVER can always be assigned routes whatever its value (D-21)
 */
public record StoreMember(long id,
                          String email,
                          String fullName,
                          String phoneNumber,
                          UserRole role,
                          UserStatus status,
                          boolean canDrive) {}
