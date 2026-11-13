package uk.huy.pathwise.notification.infrastructure.sender;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;

import java.net.ConnectException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultEmailSenderTest {
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final FailureLog failureLog = mock(FailureLog.class);
    private final DefaultEmailSender sender = new DefaultEmailSender(mailSender, failureLog);

    @Test
    void send_whenServerUnreachable_reportSendFailedWithoutTheRecipient() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("Mail server connection failed for an@example.com",
                new ConnectException("Connection refused"))).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send("no-reply@pathwise.test", "PathWise", "an@example.com", "Mã", "<p>1</p>", true))
                .isInstanceOf(EmailSenderException.class);

        verify(failureLog).record(FailureSource.EMAIL, "smtp.send", "send_failed", "cause=ConnectException", null);
    }

    @Test
    void send_whenAuthenticationRefused_reportAuthenticationFailed() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailAuthenticationException("535 bad credentials")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send("no-reply@pathwise.test", "PathWise", "an@example.com", "Mã", "<p>1</p>", true))
                .isInstanceOf(EmailSenderException.class);

        verify(failureLog).record(FailureSource.EMAIL, "smtp.send", "authentication_failed",
                "cause=MailAuthenticationException", null);
    }
}
