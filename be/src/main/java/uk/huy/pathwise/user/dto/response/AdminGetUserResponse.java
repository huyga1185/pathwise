package uk.huy.pathwise.user.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;

public record AdminGetUserResponse(long id,
                                   String email,
                                   @Nullable String phoneNumber,
                                   UserRole role,
                                   Instant createdAt,
                                   Instant updatedAt) {
}
