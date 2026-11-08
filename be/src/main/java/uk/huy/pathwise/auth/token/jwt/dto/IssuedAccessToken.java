package uk.huy.pathwise.auth.token.jwt.dto;

import java.time.Instant;

public record IssuedAccessToken(String token, Instant expiresAt) {}
