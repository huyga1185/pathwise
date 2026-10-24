package uk.huy.pathwise.auth.token.opaque.refreshtoken.dto;

import java.time.Instant;

/**
 * One active session (refresh token family, P-07). {@code lastUsedAt} is when its current token was issued,
 * i.e. the last log-in or refresh.
 */
public record SessionDetail(String sessionId, String userAgent, String ipAddress, Instant lastUsedAt, Instant expiresAt) {}
