package uk.huy.pathwise.support;

import uk.huy.pathwise.auth.email.infrastructure.sender.EmailSender;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Records emails instead of sending them, so integration tests can read OTPs and invitation codes.
 */
public class FakeEmailSender implements EmailSender {
    public record SentEmail(String from, String name, String to, String subject, String content, boolean isHtml) {}

    private final List<SentEmail> sent = new CopyOnWriteArrayList<>();

    @Override
    public void send(String from, String name, String to, String subject, String content, boolean isHtml) {
        sent.add(new SentEmail(from, name, to, subject, content, isHtml));
    }

    public List<SentEmail> sentTo(String to) {
        return sent.stream().filter(email -> email.to().equals(to)).toList();
    }

    public List<SentEmail> all() {
        return List.copyOf(sent);
    }

    public void clear() {
        sent.clear();
    }
}
