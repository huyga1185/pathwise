package uk.huy.pathwise.notification.service;

/**
 * The email could not be delivered to the mail server (technical failure, P-24). Callers decide what it means for
 * their use case, e.g. auth releases the OTP cooldown so the user can ask again.
 */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
