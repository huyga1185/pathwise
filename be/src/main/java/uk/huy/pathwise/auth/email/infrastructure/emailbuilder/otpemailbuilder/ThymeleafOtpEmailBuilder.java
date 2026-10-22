package uk.huy.pathwise.auth.email.infrastructure.emailbuilder.otpemailbuilder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import uk.huy.pathwise.auth.email.infrastructure.emailcontext.otpcontext.OtpEmailContext;

@Component
@RequiredArgsConstructor
public class ThymeleafOtpEmailBuilder implements OtpEmailBuilder {
    private final OtpEmailContext otpEmailContext;
    private final TemplateEngine templateEngine;

    @Override
    public String buildEmail(String targetEmail, String otp) {
        Context context = new Context();

        context.setVariable("userEmail", targetEmail);
        context.setVariable("otp", otp);
        context.setVariable("otpValidTime", otpEmailContext.getOtpValidTime());

        context.setVariable("appName", otpEmailContext.getAppName());
        context.setVariable("appUrl", otpEmailContext.getAppUrl());
        context.setVariable("companyName", otpEmailContext.getCompanyName());
        context.setVariable("companyUrl", otpEmailContext.getCompanyUrl());
        context.setVariable("companyAddress", otpEmailContext.getCompanyAddress());

        return templateEngine.process("email/otp", context);
    }
}
