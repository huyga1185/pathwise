package uk.huy.pathwise.notification.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import uk.huy.pathwise.notification.infrastructure.config.MailSenderProperties;
import uk.huy.pathwise.notification.infrastructure.config.ServiceIdentityProperties;
import uk.huy.pathwise.notification.infrastructure.sender.EmailSender;
import uk.huy.pathwise.notification.infrastructure.sender.EmailSenderException;
import uk.huy.pathwise.notification.infrastructure.sender.InlineImage;

import java.util.List;
import java.util.Map;

/**
 * Public API of the {@code notification} module (ARCHITECTURE.md §2): renders an HTML template from
 * {@code templates/email/{templateName}.html} and sends it. Every template also receives the service identity
 * ({@code appName}, {@code appUrl}, {@code companyName}, {@code companyUrl}, {@code companyAddress}), {@code webUrl}
 * (the web app) and {@code to}. Every email carries the logo as an inline image, {@code cid:logo} (P-70).
 */
@Service
@EnableConfigurationProperties({MailSenderProperties.class, ServiceIdentityProperties.class})
public class EmailNotificationService {
    public static final String VERIFY_EMAIL = "verify-email";
    public static final String RESET_PASSWORD = "reset-password";
    public static final String CHANGE_PASSWORD = "change-password";
    public static final String INVITATION = "invitation";
    // T57 (P-63).
    public static final String RESET_LINK = "reset-link";
    public static final String OWNER_GRANTED = "owner-granted";
    public static final String OWNER_TRANSFERRED = "owner-transferred";

    // Subject of each template; a template without a subject here cannot be sent.
    private static final Map<String, String> SUBJECTS = Map.of(
            VERIFY_EMAIL, "Mã xác thực email",
            RESET_PASSWORD, "Mã đặt lại mật khẩu",
            CHANGE_PASSWORD, "Mã xác nhận đổi mật khẩu",
            INVITATION, "Lời mời tham gia cửa hàng",
            RESET_LINK, "Đặt lại mật khẩu",
            OWNER_GRANTED, "Bạn đã trở thành chủ cửa hàng",
            OWNER_TRANSFERRED, "Quyền chủ cửa hàng đã được chuyển");

    // The white mark on the header band of templates/email/fragments/layout.html.
    private static final List<InlineImage> INLINE_IMAGES = List.of(
            new InlineImage("logo", new ClassPathResource("email/pathwise-logo.png"), "image/png"));
    // Where the web app is served under app-url (Caddy, P-47).
    private static final String WEB_PATH = "/app/";

    private final EmailSender emailSender;
    private final TemplateEngine templateEngine;
    private final MailSenderProperties sender;
    private final ServiceIdentityProperties identity;

    public EmailNotificationService(EmailSender emailSender,
                                    TemplateEngine templateEngine,
                                    MailSenderProperties sender,
                                    ServiceIdentityProperties identity) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.sender = sender;
        this.identity = identity;
    }

    /**
     * @param templateName one of the constants of this class
     * @param to           recipient address
     * @param variables    template-specific variables
     * @throws IllegalArgumentException if the template is unknown
     * @throws EmailDeliveryException   if the mail server could not take the email
     */
    public void send(String templateName, String to, Map<String, Object> variables) {
        String subject = SUBJECTS.get(templateName);
        if (subject == null) throw new IllegalArgumentException("Unknown email template: " + templateName);

        Context context = new Context();
        context.setVariables(variables);
        context.setVariable("to", to);
        context.setVariable("appName", identity.appName());
        context.setVariable("appUrl", identity.appUrl());
        context.setVariable("webUrl", identity.appUrl().replaceAll("/+$", "") + WEB_PATH);
        context.setVariable("companyName", identity.companyName());
        context.setVariable("companyUrl", identity.companyUrl());
        context.setVariable("companyAddress", identity.companyAddress());
        String content = templateEngine.process("email/" + templateName, context);

        try {
            emailSender.send(sender.address(), sender.name(), to, subject + " - " + identity.appName(), content, true,
                    INLINE_IMAGES);
        } catch (EmailSenderException e) {
            throw new EmailDeliveryException("Could not send email " + templateName, e);
        }
    }

    /**
     * @return {@code service-identity.app-url}, for links in emails
     */
    public String appUrl() {
        return identity.appUrl();
    }
}
