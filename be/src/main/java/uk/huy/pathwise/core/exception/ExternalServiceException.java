package uk.huy.pathwise.core.exception;

import lombok.Getter;

/**
 * Thrown by providers when an external system (Nominatim, OSRM, SMTP...) fails or times out.
 * Mapped to HTTP 502 with the given {@link ErrorCode} (e.g. {@code ROUTING_PROVIDER_ERROR}).
 */
@Getter
public class ExternalServiceException extends RuntimeException {
    private final ErrorCode errorCode;

    public ExternalServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
