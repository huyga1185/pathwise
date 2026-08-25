package uk.huy.pathwise.auth.email.service;

import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.email.infrastructure.emailcontext.otpcontext.OtpEmailContext;
import uk.huy.pathwise.auth.email.infrastructure.emailbuilder.otpemailbuilder.OtpEmailBuilder;
import uk.huy.pathwise.auth.email.infrastructure.sender.EmailSender;

@Service
public class OtpEmailService {
    private final EmailSender emailSender;
    private final OtpEmailBuilder otpEmailBuilder;
    private final OtpEmailContext otpEmailContext;

    public OtpEmailService(EmailSender emailSender, OtpEmailBuilder otpEmailBuilder, OtpEmailContext otpEmailContext) {
        this.emailSender = emailSender;
        this.otpEmailBuilder = otpEmailBuilder;
        this.otpEmailContext = otpEmailContext;
    }

    public void send(String targetEmail, String otp) {
        emailSender.send(otpEmailContext.getFrom(),
                otpEmailContext.getName(),
                targetEmail,
                String.format("Your %s otp code", otpEmailContext.getAppName()),
                otpEmailBuilder.buildEmail(targetEmail, otp), true);
    }
}
