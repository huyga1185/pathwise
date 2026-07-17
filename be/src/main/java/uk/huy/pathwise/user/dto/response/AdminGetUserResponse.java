package uk.huy.pathwise.user.dto.response;

import java.time.Instant;

public record AdminGetUserResponse(long id,
                                   String email,
                                   String phoneNumber,
                                   Instant createdAt,
                                   Instant updatedAt) {
}
