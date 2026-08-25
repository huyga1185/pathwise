package uk.huy.pathwise.auth.otp;

import java.util.Optional;

public interface OtpService {
    Optional<String> generateAndStoreOtp(String purpose, String identifier);
    boolean verifyOtp(String rawOtp, String purpose, String identifier);
}
