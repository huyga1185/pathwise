package uk.huy.pathwise.user.dto.response;

import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;

public record AdminGetUserResponse(long id,
                                   String email,
                                   String phoneNumber,
                                   UserRole role,
                                   Instant createdAt,
                                   Instant updatedAt) {
}
