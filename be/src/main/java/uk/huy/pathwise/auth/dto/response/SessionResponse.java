package uk.huy.pathwise.auth.dto.response;

import java.time.Instant;

/**
 * {@code SessionResponse} of API_SPEC §2. {@code lastUsedAt} is the last log-in or refresh of the session.
 */
public record SessionResponse(String sessionId,
                              String userAgent,
                              String ipAddress,
                              Instant lastUsedAt,
                              Instant expiresAt,
                              boolean current) {}
