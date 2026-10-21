package uk.huy.pathwise.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Maps every exception raised inside Spring MVC to the error format of API_SPEC §1.
 * Errors raised by the security filter chain (401/403) are written by the handlers in
 * {@code auth.infrastructure.security} using the same {@link ErrorResponseFactory}.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private final ErrorResponseFactory errors;

    public GlobalExceptionHandler(Environment env) {
        this.errors = new ErrorResponseFactory(env);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ae) {
        return errors.response(ae.getErrorCode(), null, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> details = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        return errors.response(ErrorCode.VALIDATION_ERROR, details, null);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        List<FieldErrorDetail> details = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorDetail(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return errors.response(ErrorCode.VALIDATION_ERROR, details, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception e) {
        return errors.response(ErrorCode.MALFORMED_REQUEST, null, e);
    }

    /**
     * Every cookie read by controllers carries a credential (refresh token, OTP ticket), so a missing
     * cookie is treated as an invalid token rather than a malformed request.
     */
    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingCookie(MissingRequestCookieException e) {
        return errors.response(ErrorCode.INVALID_TOKEN, null, e);
    }

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccessDenied(Exception e) {
        return errors.response(ErrorCode.FORBIDDEN, null, e);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
        return errors.response(ErrorCode.NOT_FOUND, null, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return errors.response(ErrorCode.METHOD_NOT_ALLOWED, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return errors.response(ErrorCode.CONFLICT, null, e);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalService(ExternalServiceException e) {
        log.warn("External service failure ({}): {}", e.getErrorCode(), e.getMessage(), e.getCause());
        return errors.response(e.getErrorCode(), null, e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return errors.response(ErrorCode.INTERNAL_ERROR, null, e);
    }
}
