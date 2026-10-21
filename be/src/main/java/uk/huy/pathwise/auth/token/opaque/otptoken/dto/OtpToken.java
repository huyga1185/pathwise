package uk.huy.pathwise.auth.token.opaque.otptoken.dto;

import java.time.Instant;

public record OtpToken(String token, String ticket, Instant expiresAfter) {}
