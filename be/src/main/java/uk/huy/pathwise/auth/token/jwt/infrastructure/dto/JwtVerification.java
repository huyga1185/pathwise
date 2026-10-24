package uk.huy.pathwise.auth.token.jwt.infrastructure.dto;

/**
 * Outcome of verifying a JWT (P-24). Technical failures (crypto, configuration) are thrown, not returned.
 */
public sealed interface JwtVerification {
    record Verified(JwtClaimsSet claims) implements JwtVerification {}

    record Rejected(JwtRejectReason reason) implements JwtVerification {}
}
