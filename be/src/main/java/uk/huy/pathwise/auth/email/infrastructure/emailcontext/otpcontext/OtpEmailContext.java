package uk.huy.pathwise.auth.email.infrastructure.emailcontext.otpcontext;

import lombok.Getter;
import uk.huy.pathwise.auth.email.infrastructure.emailcontext.EmailContext;

@Getter
public class OtpEmailContext extends EmailContext {
    private final String otpValidTime;

    public OtpEmailContext(String from, String name, String appName, String appUrl, String companyName, String companyUrl, String companyAddress, String otpValidTime) {
        super(from, name, appName, appUrl, companyName, companyUrl, companyAddress);
        this.otpValidTime = otpValidTime;
    }
}
