package uk.huy.pathwise.auth.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.huy.pathwise.auth.model.ClientDetail;

import java.io.IOException;

/**
 * Not a Spring bean on purpose: see {@link JwtAuthFilter}.
 */
@Slf4j
public class ClientDetailFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        log.debug("In ClientDetailFilter: User-Agent:{}", request.getHeader("User-Agent"));

        // The proxy's X-Forwarded-For is applied to the remote address only when server.forward-headers-strategy is
        // set behind a trusted proxy (F-21); the header itself is never read here, so a client cannot choose its IP.
        ClientDetail clientDetail = new ClientDetail(request.getHeader("User-Agent"), request.getRemoteAddr());
        request.setAttribute("clientDetail", clientDetail);
        filterChain.doFilter(request, response);
    }
}
