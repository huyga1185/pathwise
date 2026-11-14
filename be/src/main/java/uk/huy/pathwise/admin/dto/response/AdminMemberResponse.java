package uk.huy.pathwise.admin.dto.response;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;

import java.time.Instant;

/**
 * A member in the ADMIN store detail: no name or phone number (P-59).
 */
public record AdminMemberResponse(long id, String email, UserRole role, UserStatus status, Instant createdAt) {}
