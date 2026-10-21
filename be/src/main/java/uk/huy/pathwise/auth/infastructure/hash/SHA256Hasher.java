package uk.huy.pathwise.auth.infastructure.hash;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * {@inheritDoc}
 *
 * <p><b>Implementation Details:</b>
 * Uses the SHA-256 algorithm (unsalted) and encodes the output as a Hexadecimal string (64 characters).
 *
 * <p><b>Security Warning & Usage Constraints:</b>
 * Because SHA-256 is a high-speed cryptographic hash function and this implementation
 * does not apply salting, it <b>MUST ONLY</b> be used to hash high-entropy,
 * machine-generated secrets (e.g., Opaque Tokens, UUIDs).
 * <br>
 * <b>STRICTLY DO NOT</b> use this class for hashing user passwords.
 * For user password management, rely on Spring Security's standard
 * {@code PasswordEncoder} interface instead of this interface.
 */
@Component
public class SHA256Hasher implements Hasher {
    @Override
    public String hashString(String rawString) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA256 is not available", e);
        }
        return HexFormat.of().formatHex(messageDigest.digest(rawString.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Implementation Note:</b>
     * Utilizes {@code MessageDigest.isEqual} to prevent Timing Attacks,
     * strictly adhering to the security contract defined in the Interface.
     */
    @Override
    public boolean match(String hashedString, String rawString) {
        if (hashedString == null || rawString == null) {
            return false;
        }

        // Convert both to byte arrays to ensure constant-time comparison
        byte[] expectedHashBytes = hashedString.getBytes(StandardCharsets.UTF_8);
        byte[] actualHashBytes = hashString(rawString).getBytes(StandardCharsets.UTF_8);

        // MANDATORY: Prevents timing attacks
        return MessageDigest.isEqual(expectedHashBytes, actualHashBytes);
    }
}
