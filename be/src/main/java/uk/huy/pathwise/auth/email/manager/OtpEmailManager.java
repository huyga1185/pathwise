package uk.huy.pathwise.auth.email.manager;

import org.springframework.stereotype.Component;
import uk.huy.pathwise.auth.email.infrastructure.emailcontext.otpcontext.OtpEmailContext;
import uk.huy.pathwise.auth.email.infrastructure.emailbuilder.otpemailbuilder.OtpEmailBuilder;
import uk.huy.pathwise.auth.email.infrastructure.sender.EmailSender;

@Component
public class OtpEmailManager {
    private final EmailSender emailSender;
    private final OtpEmailBuilder otpEmailBuilder;
    private final OtpEmailContext otpEmailContext;

    public OtpEmailManager(EmailSender emailSender, OtpEmailBuilder otpEmailBuilder, OtpEmailContext otpEmailContext) {
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
