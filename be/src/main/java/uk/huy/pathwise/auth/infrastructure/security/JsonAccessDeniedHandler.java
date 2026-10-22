package uk.huy.pathwise.auth.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ErrorResponseFactory;

import java.io.IOException;

/**
 * Writes 403 {@code FORBIDDEN} in the standard error format for access denied in the filter chain.
 * Method-security denials ({@code @PreAuthorize}) are handled by {@code GlobalExceptionHandler}.
 * Instantiated by {@code SecurityConfig} (not a bean).
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private final ErrorResponseFactory errors;
    private final ObjectMapper objectMapper;

    public JsonAccessDeniedHandler(ErrorResponseFactory errors, ObjectMapper objectMapper) {
        this.errors = errors;
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getHttpCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errors.body(ErrorCode.FORBIDDEN, null, null));
    }
}
