package uk.huy.pathwise.auth.email.infrastructure.emailcontext.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.huy.pathwise.auth.email.infrastructure.emailcontext.otpcontext.OtpEmailContext;

@Configuration
public class EmailContextConfig {
    @Bean
    public OtpEmailContext otpEmailContext(@Value("${mail.otp.sender.address}") String from,
                                           @Value("${mail.otp.sender.name}") String name,
                                           @Value("${otp.expiration-time:300}") String otpValidTime,
                                           @Value("${service.identity.app-name}") String appName,
                                           @Value("${service.identity.app-url}") String appUrl,
                                           @Value("${service.identity.company-name}") String companyName,
                                           @Value("${service.identity.company-url}") String companyUrl,
                                           @Value("${service.identity.company-address}") String companyAddress) {
        return new OtpEmailContext(from,
                name,
                appName,
                appUrl,
                companyName,
                companyUrl,
                companyAddress,
                otpValidTime);
    }
}
