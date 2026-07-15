package uk.huy.pathwise.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.*;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private final Environment env;

    public GlobalExceptionHandler(Environment env) {
        this.env = env;
    }

    private record FieldErrorInfo(String field, String message) {};

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponse> handleMANVE(MethodArgumentNotValidException manve) {
        List<FieldErrorInfo> errors = new ArrayList<>();
        manve.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(new FieldErrorInfo(error.getField(), error.getDefaultMessage()));
        });
        return build(
                "VALIDATION_ERROR",
                errors,
                HttpStatus.BAD_REQUEST,
                null
        );
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ae) {
        return build(
                ae.getErrorCode().name(),
                null,
                HttpStatus.valueOf(ae.getErrorCode().getHttpCode()),
                null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled exception", e);
        return build(
                null,
                null,
                HttpStatus.INTERNAL_SERVER_ERROR,
                e);
    }

    private ResponseEntity<ErrorResponse> build(
            String message,
            Object detail,
            HttpStatus httpStatus,
            Exception e) {
        boolean isDev = e != null && env.acceptsProfiles(Profiles.of("dev"));
        Object finalDetail = detail;
        if (detail == null && isDev) {
            finalDetail = e.getClass().getName();
        }
        ErrorResponse body = new ErrorResponse(
                message,
                finalDetail,
                isDev ? e.getMessage() : null,
                isDev ? Arrays.stream(e.getStackTrace()).limit(10).map(StackTraceElement::toString).toList() : null
        );
        return ResponseEntity.status(httpStatus).body(body);
    }
}
