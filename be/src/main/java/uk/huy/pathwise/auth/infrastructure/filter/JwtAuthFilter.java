package uk.huy.pathwise.auth.infrastructure.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.huy.pathwise.auth.token.jwt.dto.AccessTokenVerification;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtRejectReason;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;

import java.io.IOException;
import java.util.List;

/**
 * Not a Spring bean on purpose: it is instantiated by {@code SecurityConfig} so that it only runs inside
 * the security filter chain (not also as a servlet filter) and is not picked up by {@code @WebMvcTest} slices.
 */
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
    private final AccessTokenManager accessTokenManager;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        switch (accessTokenManager.verifyAccessToken(header.substring(7))) {
            case AccessTokenVerification.Authenticated authenticated -> authenticate(authenticated.user());
            // Rejected: the request continues anonymously; protected endpoints then answer 401.
            case AccessTokenVerification.Rejected rejected -> {
                if (rejected.reason() == JwtRejectReason.EXPIRED) log.debug("Access token rejected: {}", rejected.reason());
                else log.warn("Access token rejected: {}", rejected.reason());
            }
        }

        filterChain.doFilter(request, response);
    }

    private static void authenticate(AuthenticatedUser user) {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.identity().role().name()));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, authorities));
    }
}
