package uk.huy.pathwise.auth.token.opaque.refreshtoken.dto;

import java.time.Instant;

/**
 * Outcome of rotating a refresh token (P-24). A rejection is a normal outcome: the manager's transaction still
 * commits the state it changed (EXPIRED, revoked family), which a thrown exception would roll back.
 */
public sealed interface RotationResult {
    record Rotated(long userId, String sessionId, String token, Instant expiresAt) implements RotationResult {}

    record Rejected(RejectReason reason) implements RotationResult {}

    enum RejectReason {
        MISSING, NOT_FOUND, EXPIRED, REVOKED_REUSED, USED_REUSED, INCONSISTENT_FAMILY
    }
}
