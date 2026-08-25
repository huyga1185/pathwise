package uk.huy.pathwise.auth.email.infrastructure.emailbuilder.otpemailbuilder;

public interface OtpEmailBuilder {
    String buildEmail(String targetEmail, String otp);
}
