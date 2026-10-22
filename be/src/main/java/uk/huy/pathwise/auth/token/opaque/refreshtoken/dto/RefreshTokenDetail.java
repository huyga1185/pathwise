package uk.huy.pathwise.auth.token.opaque.refreshtoken.dto;

import java.time.Instant;

public record RefreshTokenDetail(long id, String userAgent, String ipAddress, Instant expiresAt, Instant createdAt) {}
