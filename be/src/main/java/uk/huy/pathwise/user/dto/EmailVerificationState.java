package uk.huy.pathwise.user.dto;

import uk.huy.pathwise.shared.identity.UserStatus;

/**
 * What the email verification flow needs to know about an account.
 */
public record EmailVerificationState(long userId, String email, UserStatus status, boolean emailVerified) {}
