package uk.huy.pathwise.notification.infrastructure.sender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Sends through the SMTP server of {@code spring.mail.*}. Failures are reported to {@link FailureLog} (T54) without
 * the recipient: the server's message may contain the address, so only the type of the root cause is kept.
 */
@Component
public class DefaultEmailSender implements EmailSender {
    private static final String OPERATION = "smtp.send";
    private final JavaMailSender javaMailSender;
    private final FailureLog failureLog;

    public DefaultEmailSender(JavaMailSender javaMailSender, FailureLog failureLog) {
        this.javaMailSender = javaMailSender;
        this.failureLog = failureLog;
    }

    @Override
    public void send(String from, String name, String to, String subject, String content, boolean isHtml) {
        if (from == null) throw new NullPointerException("From could not be null");
        if (from.isEmpty()) throw new IllegalArgumentException("From could not be empty");

        if (to == null) throw new NullPointerException("To could not be null");
        if (to.isEmpty()) throw new IllegalArgumentException("To could not be empty");

        if (subject == null) throw new NullPointerException("Subject could not be null");
        if (subject.isEmpty()) throw new IllegalArgumentException("Subject could not be empty");

        if (content == null) throw new NullPointerException("Content could not be null");
        if (content.isEmpty()) throw new IllegalArgumentException("Content could not be empty");

        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        // UTF-8: templates are Vietnamese.
        MimeMessageHelper mimeMessageHelper;
        try {
            mimeMessageHelper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
            if (name == null || name.isBlank()) mimeMessageHelper.setFrom(from);
            else mimeMessageHelper.setFrom(from, name);
            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(content, isHtml);
        } catch (MessagingException | UnsupportedEncodingException e) {
            failureLog.record(FailureSource.EMAIL, OPERATION, "invalid_message", "", e);
            throw new EmailSenderException("Could not create mime message");
        }
        try {
            javaMailSender.send(mimeMessage);
        } catch (MailAuthenticationException e) {
            failureLog.record(FailureSource.EMAIL, OPERATION, "authentication_failed", rootCause(e), null);
            throw new EmailSenderException("Could not send email");
        } catch(MailSendException e) {
            failureLog.record(FailureSource.EMAIL, OPERATION, "send_failed", rootCause(e), null);
            throw new EmailSenderException("Could not send email");
        }
    }

    private static String rootCause(Exception e) {
        return "cause=" + NestedExceptionUtils.getMostSpecificCause(e).getClass().getSimpleName();
    }
}
