package uk.huy.pathwise.notification.infrastructure.sender;

import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;

import java.net.ConnectException;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
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

        assertThatThrownBy(() -> sender.send("no-reply@pathwise.test", "PathWise", "an@example.com", "Mã", "<p>1</p>", true, List.of()))
                .isInstanceOf(EmailSenderException.class);

        verify(failureLog).record(FailureSource.EMAIL, "smtp.send", "send_failed", "cause=ConnectException", null);
    }

    @Test
    void send_whenAuthenticationRefused_reportAuthenticationFailed() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailAuthenticationException("535 bad credentials")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send("no-reply@pathwise.test", "PathWise", "an@example.com", "Mã", "<p>1</p>", true, List.of()))
                .isInstanceOf(EmailSenderException.class);

        verify(failureLog).record(FailureSource.EMAIL, "smtp.send", "authentication_failed",
                "cause=MailAuthenticationException", null);
    }

    @Test
    void send_whenInlineImage_buildRelatedMultipartWithTheImageByContentId() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        InlineImage logo = new InlineImage("logo", new ByteArrayResource(new byte[]{1, 2, 3}), "image/png");

        sender.send("no-reply@pathwise.test", "PathWise", "an@example.com", "Mã", "<img src=\"cid:logo\">", true, List.of(logo));

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        MimeMessage message = sent.getValue();
        message.saveChanges();
        assertThat(message.getContentType()).startsWith("multipart/related");
        MimeMultipart related = (MimeMultipart) message.getContent();
        MimeBodyPart image = (MimeBodyPart) related.getBodyPart(1);
        assertThat(image.getContentID()).isEqualTo("<logo>");
        assertThat(image.getContentType()).startsWith("image/png");
        assertThat(image.getDisposition()).isEqualTo(Part.INLINE);
    }

    @Test
    void send_whenNoInlineImage_sendSinglePartHtml() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        sender.send("no-reply@pathwise.test", "PathWise", "an@example.com", "Mã", "<p>1</p>", true, List.of());

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());
        sent.getValue().saveChanges();
        assertThat(sent.getValue().getContentType()).startsWith("text/html");
    }
}
