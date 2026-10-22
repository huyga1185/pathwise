package uk.huy.pathwise.auth.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import uk.huy.pathwise.auth.infrastructure.filter.ClientDetailFilter;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.auth.infrastructure.filter.JwtAuthFilter;
import uk.huy.pathwise.auth.infrastructure.security.JsonAccessDeniedHandler;
import uk.huy.pathwise.auth.infrastructure.security.JsonAuthenticationEntryPoint;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.core.exception.ErrorResponseFactory;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final AccessTokenManager accessTokenManager;
    private final Environment environment;
    private final ObjectMapper objectMapper;

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
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/log-out").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new JsonAuthenticationEntryPoint(errors, objectMapper))
                        .accessDeniedHandler(new JsonAccessDeniedHandler(errors, objectMapper)))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new ClientDetailFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthFilter(accessTokenManager), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
