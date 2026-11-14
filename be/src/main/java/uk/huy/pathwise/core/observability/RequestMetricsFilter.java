package uk.huy.pathwise.core.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Counts every API request with its status and time in {@link OpsMetrics} (T60), grouped by the first path segment
 * after {@code /api/v1} ({@code plans}, {@code driver}, {@code admin}…). Runs just after the log context, before
 * security, so refused requests count too. In a {@code @WebMvcTest} slice there is no {@link OpsMetrics}: it does
 * nothing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestMetricsFilter extends OncePerRequestFilter {
    private static final String PREFIX = "/api/v1/";

    private final ObjectProvider<OpsMetrics> metrics;

    public RequestMetricsFilter(ObjectProvider<OpsMetrics> metrics) {
        this.metrics = metrics;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            OpsMetrics opsMetrics = metrics.getIfAvailable();
            if (opsMetrics != null)
                opsMetrics.recordRequest(groupOf(request.getRequestURI()), response.getStatus(), System.nanoTime() - start);
        }
    }

    static String groupOf(String uri) {
        String rest = uri.substring(PREFIX.length());
        int slash = rest.indexOf('/');
        String group = slash < 0 ? rest : rest.substring(0, slash);
        // Only names of the API, never an id or anything a client made up.
        return group.matches("[a-z-]{1,20}") ? group : "other";
    }
}
