package uk.huy.pathwise.auth.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * {@code SessionResponse} of API_SPEC §2. {@code lastUsedAt} is the last log-in or refresh of the session.
 */
public record SessionResponse(String sessionId,
                              @Nullable String userAgent,
                              @Nullable String ipAddress,
                              Instant lastUsedAt,
                              Instant expiresAt,
                              boolean current) {}
