package uk.huy.pathwise.support;

import uk.huy.pathwise.notification.infrastructure.sender.EmailSender;
import uk.huy.pathwise.notification.infrastructure.sender.EmailSenderException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Records emails instead of sending them, so integration tests can read OTPs and invitation codes.
 */
public class FakeEmailSender implements EmailSender {
    public record SentEmail(String from, String name, String to, String subject, String content, boolean isHtml) {}

    private final List<SentEmail> sent = new CopyOnWriteArrayList<>();
    private final AtomicBoolean failNext = new AtomicBoolean();

    @Override
    public void send(String from, String name, String to, String subject, String content, boolean isHtml) {
        if (failNext.getAndSet(false)) throw new EmailSenderException("Simulated SMTP failure");
        sent.add(new SentEmail(from, name, to, subject, content, isHtml));
    }

    public List<SentEmail> sentTo(String to) {
        return sent.stream().filter(email -> email.to().equals(to)).toList();
    }

    public List<SentEmail> all() {
        return List.copyOf(sent);
    }

    /**
     * The next {@link #send} fails like an unreachable SMTP server, then sending works again.
     */
    public void failNextSend() {
        failNext.set(true);
    }

    public void clear() {
        sent.clear();
    }
}
