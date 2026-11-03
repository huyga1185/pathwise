package uk.huy.pathwise.auth.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import uk.huy.pathwise.auth.infrastructure.filter.ClientDetailFilter;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.auth.infrastructure.filter.JwtAuthFilter;
import uk.huy.pathwise.auth.infrastructure.security.AuthorizationRules;
import uk.huy.pathwise.auth.infrastructure.security.JsonAccessDeniedHandler;
import uk.huy.pathwise.auth.infrastructure.security.JsonAuthenticationEntryPoint;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.core.exception.ErrorResponseFactory;
import uk.huy.pathwise.core.web.LogContextFilter;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {
    private static final List<String> CORS_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private static final List<String> CORS_REQUEST_HEADERS = List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
            "X-Client-Platform");
    private static final List<String> CORS_EXPOSED_HEADERS = List.of(HttpHeaders.RETRY_AFTER, LogContextFilter.REQUEST_ID_HEADER);

    private final AccessTokenManager accessTokenManager;
    private final Environment environment;
    private final ObjectMapper objectMapper;
    private final CorsProperties corsProperties;

    /**
     * Rules used in {@code @PreAuthorize} expressions as {@code @authz...}.
     */
    @Bean("authz")
    public AuthorizationRules authorizationRules() {
        return new AuthorizationRules();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) {
        ErrorResponseFactory errors = new ErrorResponseFactory(environment);
        return httpSecurity
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/actuator/health/**",
                                "/error",
                                "/api/v1/auth/log-in",
                                "/api/v1/auth/register",
                                "/api/v1/auth/email/verify",
                                "/api/v1/auth/email/verify/resend",
                                "/api/v1/auth/password/forgot",
                                "/api/v1/auth/password/reset/verify",
                                "/api/v1/auth/password/reset",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/log-out",
                                "/api/v1/invitations/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint(errors, objectMapper))
                        .accessDeniedHandler(new JsonAccessDeniedHandler(errors, objectMapper)))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Spring Security defaults stay on: nosniff, X-Frame-Options DENY, Cache-Control no-store, HSTS over HTTPS.
                .headers(headers -> headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new ClientDetailFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthFilter(accessTokenManager), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Not a bean, like the other parts of the chain (P-23): only the web origins of {@code cors.allowed-origins},
     * with credentials for the refresh token cookie (P-04).
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(CORS_METHODS);
        configuration.setAllowedHeaders(CORS_REQUEST_HEADERS);
        configuration.setExposedHeaders(CORS_EXPOSED_HEADERS);
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
