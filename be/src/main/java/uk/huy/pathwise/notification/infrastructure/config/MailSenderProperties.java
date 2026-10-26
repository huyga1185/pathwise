package uk.huy.pathwise.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Sender of every email. The key keeps its historical name {@code mail.otp.sender.*} (env
 * {@code MAIL_OTP_SENDER_ADDRESS}, {@code MAIL_OTP_SENDER_NAME}) so existing deployments keep working.
 */
@ConfigurationProperties("mail.otp.sender")
public record MailSenderProperties(String address, String name) {}
