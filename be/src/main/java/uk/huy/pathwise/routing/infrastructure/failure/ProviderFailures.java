package uk.huy.pathwise.routing.infrastructure.failure;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;

import java.util.function.Supplier;

/**
 * Reports provider failures to {@link FailureLog} (T54) for the decorators of this package, then rethrows them.
 * {@code GEOCODING_BUSY} is our own rate limit, not a failure of the server, and is not reported.
 */
final class ProviderFailures {
    private ProviderFailures() {
    }

    static <T> T observe(FailureLog failureLog, FailureSource source, String operation, Supplier<T> call) {
        try {
            return call.get();
        } catch (ExternalServiceException e) {
            if (e.getErrorCode() != ErrorCode.GEOCODING_BUSY)
                failureLog.record(source, operation, reasonOf(e), "message=\"" + e.getMessage() + "\"", null);
            throw e;
        }
    }

    /**
     * {@code unreachable} (connection refused, timeout), {@code http_<status>}, or {@code bad_response} (a reply
     * that is not what we asked for: an OSRM error code, no route, invalid coordinates).
     */
    static String reasonOf(ExternalServiceException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ResourceAccessException) return "unreachable";
        if (cause instanceof HttpStatusCodeException http) return "http_" + http.getStatusCode().value();
        return "bad_response";
    }
}
