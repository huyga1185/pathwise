package uk.huy.pathwise.auth.dto.response;

import java.time.Instant;

public record VerifyOtpResponse(String token, Instant expiresAt) {}
