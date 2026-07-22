package uk.huy.pathwise.auth.infastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.infastructure.token.jwt.JwtService;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        Optional<UserIdentity> identity = jwtService.verifyJwt(token);
        if (identity.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }
        String role = identity.get().role().name();

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                identity.get(),
                null,
                authorities
        );

        String ip;

        String requestIp = request.getHeader("X-Forwarded-For");
        if (requestIp != null && !requestIp.isBlank()) {
            ip = requestIp.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }

        authenticationToken.setDetails(new ClientDetail(request.getHeader("User-Agent"), ip));

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        filterChain.doFilter(request, response);
    }
}
