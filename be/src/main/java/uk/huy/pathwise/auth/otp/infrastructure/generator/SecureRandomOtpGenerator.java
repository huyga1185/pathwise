package uk.huy.pathwise.auth.otp.infrastructure.generator;

import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

@Component
public class SecureRandomOtpGenerator implements OtpGenerator {
    private final SecureRandom secureRandom;

    public SecureRandomOtpGenerator() throws NoSuchAlgorithmException {
        Security.setProperty("securerandom.drbg.config", "CTR_DRBG");
        this.secureRandom = SecureRandom.getInstance("DRBG");
    }

    @Override
    public String generateOtp(int size) {
        if (size < 1) throw new IllegalArgumentException("Size must be >= 1");

        int min = (int) Math.pow(10, size - 1);
        int max = (int) Math.pow(10, size);

        return String.valueOf(secureRandom.nextInt(min, max));
    }
}
