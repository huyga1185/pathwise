package uk.huy.pathwise.core.exception;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;

/**
 * Builds {@link ErrorResponse} bodies so that {@link GlobalExceptionHandler} and the security handlers
 * (which run outside Spring MVC) produce exactly the same format.
 * <p>
 * Deliberately not a Spring bean: callers instantiate it with the {@link Environment}, which keeps
 * {@code @WebMvcTest} slices free of extra component dependencies.
 */
public class ErrorResponseFactory {
    private static final int STACK_TRACE_LIMIT = 10;

    private final Environment env;

    public ErrorResponseFactory(Environment env) {
        this.env = env;
    }

    public ErrorResponse body(ErrorCode code, Object details, Exception e) {
        boolean includeDebug = e != null && env.acceptsProfiles(Profiles.of("dev"));
        return new ErrorResponse(
                code.name(),
                details,
                includeDebug ? e.getClass().getName() + ": " + e.getMessage() : null,
                includeDebug ? Arrays.stream(e.getStackTrace()).limit(STACK_TRACE_LIMIT).map(StackTraceElement::toString).toList() : null);
    }

    public ResponseEntity<ErrorResponse> response(ErrorCode code, Object details, Exception e) {
        return ResponseEntity.status(code.getHttpCode()).body(body(code, details, e));
    }
}
