package uk.huy.pathwise.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Error body for every failed request (API_SPEC §1): {@code code} is an {@link ErrorCode} name,
 * {@code details} is optional (e.g. field validation errors). {@code exception} and {@code stackTrace}
 * are only filled in the {@code dev} profile.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String code,
        Object details,
        String exception,
        List<String> stackTrace) {}
