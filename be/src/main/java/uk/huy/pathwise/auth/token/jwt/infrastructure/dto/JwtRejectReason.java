package uk.huy.pathwise.auth.token.jwt.infrastructure.dto;

public enum JwtRejectReason {
    MALFORMED, BAD_SIGNATURE, WRONG_ALGORITHM, WRONG_ISSUER, WRONG_AUDIENCE, NOT_YET_VALID, EXPIRED, INVALID_CLAIMS
}
