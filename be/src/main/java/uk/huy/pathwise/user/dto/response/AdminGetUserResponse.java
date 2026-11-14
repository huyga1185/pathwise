package uk.huy.pathwise.user.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;

import java.time.Instant;

/**
 * @param storeId null for an ADMIN, who has no store
 */
public record AdminGetUserResponse(long id,
                                   String email,
                                   @Nullable String phoneNumber,
                                   UserRole role,
                                   @Nullable Long storeId,
                                   UserStatus status,
                                   Instant createdAt,
                                   Instant updatedAt) {
}
