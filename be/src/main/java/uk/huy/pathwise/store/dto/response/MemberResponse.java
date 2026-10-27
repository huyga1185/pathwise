package uk.huy.pathwise.store.dto.response;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;

/**
 * @param canDrive true if the member can be assigned routes: always for DRIVER, otherwise the stored flag (D-21)
 */
public record MemberResponse(long id,
                             String email,
                             String fullName,
                             String phoneNumber,
                             UserRole role,
                             UserStatus status,
                             boolean canDrive) {}
