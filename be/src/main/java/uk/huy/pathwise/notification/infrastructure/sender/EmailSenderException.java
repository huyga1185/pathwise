package uk.huy.pathwise.notification.infrastructure.sender;

public class EmailSenderException extends RuntimeException {
    public EmailSenderException(String message, Throwable e) {
        super(message, e);
    }
    public EmailSenderException(String message) {
        super(message);
    }
}
