package uk.huy.pathwise.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ErrorResponseFactory;

import java.io.IOException;

/**
 * Writes 401 {@code UNAUTHORIZED} in the standard error format when a protected endpoint is called
 * without a valid access token. Instantiated by {@code SecurityConfig} (not a bean).
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ErrorResponseFactory errors;
    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ErrorResponseFactory errors, ObjectMapper objectMapper) {
        this.errors = errors;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errors.body(ErrorCode.UNAUTHORIZED, null, null));
    }
}
