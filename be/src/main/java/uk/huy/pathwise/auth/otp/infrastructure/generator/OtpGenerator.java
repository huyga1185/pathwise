package uk.huy.pathwise.auth.otp.infrastructure.generator;

public interface OtpGenerator {
    String generateOtp(int size);
}
