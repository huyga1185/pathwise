package uk.huy.pathwise.auth.dto.response;

import java.time.Instant;

/**
 * @param resetToken opaque, single use; the client must not parse it
 */
public record ResetTokenResponse(String resetToken, Instant expiresAt) {}
