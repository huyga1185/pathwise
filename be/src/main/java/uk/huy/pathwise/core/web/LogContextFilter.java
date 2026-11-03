package uk.huy.pathwise.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Log context of a request (NFR-06, ARCHITECTURE.md §10): a new {@code requestId} for every request, in the MDC and in
 * the {@code X-Request-Id} response header so a client can quote it. The id is always generated here, never taken
 * from the client, so nobody can choose what lands in the logs. {@code userId} and {@code storeId} are added once the
 * access token is verified; the whole MDC is cleared when the request ends, since threads are reused.
 * <p>
 * Runs first, before the security filter chain, so that log lines of authentication and errors carry the id too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LogContextFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID = "requestId";
    public static final String USER_ID = "userId";
    public static final String STORE_ID = "storeId";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
