package uk.huy.pathwise.auth.token.opaque.refreshtoken.dto;

import java.time.Instant;

public record RefreshTokenGenerationDetail(String token, Instant expiresAt) {}
