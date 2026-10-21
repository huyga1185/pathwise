package uk.huy.pathwise.core.exception;

/**
 * One entry of {@link ErrorResponse#details()} for validation errors; {@code code} is the constraint message (UPPER_SNAKE).
 */
public record FieldErrorDetail(String field, String code) {}
