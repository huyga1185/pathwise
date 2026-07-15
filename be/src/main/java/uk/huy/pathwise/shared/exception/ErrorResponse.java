package uk.huy.pathwise.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String message,
        Object detail,
        String exception,
        List<String> stackTrace) {}
