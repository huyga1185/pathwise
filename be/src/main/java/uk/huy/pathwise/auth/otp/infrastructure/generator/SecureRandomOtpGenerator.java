package uk.huy.pathwise.auth.otp.infrastructure.generator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Uniform numeric OTP over the full {@code 10^size} range, zero-padded (e.g. {@code 004217}).
 * Uses the platform default {@link SecureRandom}, without changing JVM-wide security properties (F-22).
 */
@Component
public class SecureRandomOtpGenerator implements OtpGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateOtp(int size) {
        if (size < 1 || size > 9) throw new IllegalArgumentException("Size must be between 1 and 9");
        int bound = (int) Math.pow(10, size);
        return String.format("%0" + size + "d", secureRandom.nextInt(bound));
    }
}
