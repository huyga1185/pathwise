package uk.huy.pathwise.core.ratelimit;

import lombok.Getter;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;

import java.time.Duration;

/**
 * 429 {@code RATE_LIMITED}, with the time left until the window ends for the {@code Retry-After} header.
 */
@Getter
public class RateLimitedException extends AppException {
    private final Duration retryAfter;

    public RateLimitedException(Duration retryAfter) {
        super(ErrorCode.RATE_LIMITED);
        this.retryAfter = retryAfter;
    }
}
