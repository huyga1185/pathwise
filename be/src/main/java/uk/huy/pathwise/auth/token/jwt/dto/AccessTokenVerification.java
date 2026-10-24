package uk.huy.pathwise.auth.token.jwt.dto;

import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtRejectReason;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

/**
 * Outcome of verifying an access token (P-24): the authenticated user, or why the token is rejected.
 */
public sealed interface AccessTokenVerification {
    record Authenticated(AuthenticatedUser user) implements AccessTokenVerification {}

    record Rejected(JwtRejectReason reason) implements AccessTokenVerification {}
}
