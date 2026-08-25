package uk.huy.pathwise.auth.token.jwt.infrastructure.dto;

import java.time.Instant;

public record GeneratedOtpToken(String token, Instant expiresAt) {}
