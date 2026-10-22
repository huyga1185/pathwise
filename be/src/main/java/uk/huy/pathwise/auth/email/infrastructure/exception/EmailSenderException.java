package uk.huy.pathwise.auth.email.infrastructure.exception;

public class EmailSenderException extends RuntimeException {
    public EmailSenderException(String message, Throwable e) {
        super(message, e);
    }
    public EmailSenderException(String message) {
        super(message);
    }
}
