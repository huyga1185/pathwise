package uk.huy.pathwise.auth.otp.manager;

import uk.huy.pathwise.auth.otp.dto.OtpVerificationGrant;

public interface OtpManager {
    void sendOtp(String purpose, String identifier);
    OtpVerificationGrant verifyOtp(String purpose, String identifier, String rawOtp);
}
