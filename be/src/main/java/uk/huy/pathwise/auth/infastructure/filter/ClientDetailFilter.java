package uk.huy.pathwise.auth.infastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.huy.pathwise.auth.model.ClientDetail;

import java.io.IOException;

@Configuration
@Slf4j
public class ClientDetailFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String ip;

        String requestIp = request.getHeader("X-Forwarded-For");
        if (requestIp != null && !requestIp.isBlank()) {
            ip = requestIp.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }

        log.debug("In ClientDetailFilter: User-Agent:{}", request.getHeader("User-Agent"));

        ClientDetail clientDetail = new ClientDetail(request.getHeader("User-Agent"), ip);
        request.setAttribute("clientDetail", clientDetail);
        filterChain.doFilter(request, response);
    }
}
