package uk.huy.pathwise.auth.token.opaque.infrastructure;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * A cryptographically secure implementation of the {@link OpaqueTokenGenerator}.
 * <p>
 * This component utilizes {@link java.security.SecureRandom} to generate high-entropy,
 * unpredictable tokens, strictly fulfilling the security requirements for opaque token
 * generation. The generated output is encoded in URL-safe Base64 without padding,
 * making it immediately ready for safe transmission in HTTP headers, URL path variables,
 * query parameters, or cookies without requiring further URL encoding.
 * </p>
 */
@Component
public class SecureRandomOpaqueTokenGenerator implements OpaqueTokenGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a secure, URL-safe opaque token.
     * <p>
     * <b>Implementation Details:</b> This method generates 32 bytes (256 bits) of
     * random data, providing a very high level of cryptographic entropy. It then
     * encodes these bytes using the Base64 URL and Filename safe alphabet. The
     * standard padding character ({@code =}) is omitted to produce a cleaner string.
     * The final output is typically a 43-character long string.
     * </p>
     *
     * @return a 256-bit, URL-safe Base64 encoded, unpadded random string
     */
    @Override
    public String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
