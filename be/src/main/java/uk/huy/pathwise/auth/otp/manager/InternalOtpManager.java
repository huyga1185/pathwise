package uk.huy.pathwise.auth.otp.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.auth.otp.dto.OtpVerificationGrant;
import uk.huy.pathwise.auth.otp.store.OtpStore;
import uk.huy.pathwise.auth.token.opaque.otptoken.manager.OtpTokenManager;

@Component
@RequiredArgsConstructor
public class InternalOtpManager implements OtpManager {
    private final OtpStore otpStore;
    private final OtpTokenManager otpTokenManager;

    @Override
    public void sendOtp(String purpose, String identifier) {

    }

    @Override
    public OtpVerificationGrant verifyOtp(String purpose, String identifier, String rawOtp) {
        return null;
    }
}
