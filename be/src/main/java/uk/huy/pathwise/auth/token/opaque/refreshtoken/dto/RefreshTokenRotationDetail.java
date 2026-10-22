package uk.huy.pathwise.auth.token.opaque.refreshtoken.dto;

import java.time.Instant;

public record RefreshTokenRotationDetail(long userId, String token, Instant expiresAt) {}
