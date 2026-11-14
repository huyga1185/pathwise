package uk.huy.pathwise.user.dto;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;

import java.time.Instant;

/**
 * A member of a store as the ADMIN area sees it (T55): no name or phone number (P-59).
 */
public record MemberAccount(long id, String email, UserRole role, UserStatus status, Instant createdAt) {}
