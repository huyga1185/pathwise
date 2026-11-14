package uk.huy.pathwise.audit.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.huy.pathwise.core.web.LogContextFilter;

/**
 * Where the current action comes from: the request id of the log context and the client IP (already taken from the
 * trusted proxy's X-Forwarded-For by Tomcat, P-38). Both null outside an HTTP request, for example in a job.
 */
public final class RequestOrigin {
    private RequestOrigin() {
    }

    public static @Nullable String requestId() {
        return MDC.get(LogContextFilter.REQUEST_ID);
    }

    public static @Nullable String ip() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request.getRemoteAddr();
        }
        return null;
    }
}
