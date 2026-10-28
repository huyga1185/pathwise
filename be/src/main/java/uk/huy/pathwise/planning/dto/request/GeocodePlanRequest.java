package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * {@code POST /plans/{id}/geocode}.
 *
 * @param version the version last read
 */
public record GeocodePlanRequest(@NotNull(message = "VERSION_REQUIRED") Long version) {}
