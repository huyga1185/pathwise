package uk.huy.pathwise.auth.email.infrastructure.sender;

public interface EmailSender {
    void send(String from, String name, String to, String subject, String content, boolean isHtml);
}
