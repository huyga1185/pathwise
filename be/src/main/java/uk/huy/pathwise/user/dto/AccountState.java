package uk.huy.pathwise.user.dto;

import uk.huy.pathwise.shared.identity.UserStatus;

/**
 * Status of an account looked up by email, for the email verification and password reset flows.
 */
public record AccountState(long userId, String email, UserStatus status, boolean emailVerified) {}
