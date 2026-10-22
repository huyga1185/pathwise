package uk.huy.pathwise.auth.email.infrastructure.sender;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.auth.email.infrastructure.exception.EmailSenderException;

@Component
@Slf4j
public class DefaultEmailSender implements EmailSender {
    private final JavaMailSender javaMailSender;

    public DefaultEmailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
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
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
        try {
            mimeMessageHelper.setFrom(from);
            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(content, isHtml);
        } catch (MessagingException e) {
            log.error("Could not create mime message", e);
            throw new EmailSenderException("Could not create mime message");
        }
        try {
            javaMailSender.send(mimeMessage);
        } catch (MailAuthenticationException e) {
            log.error("Mail server authentication failure", e);
            throw new EmailSenderException("Could not send email");
        } catch(MailSendException e) {
            log.error("An unexpected error occurred when sending email", e);
            throw new EmailSenderException("Could not send email");
        }
    }
}
