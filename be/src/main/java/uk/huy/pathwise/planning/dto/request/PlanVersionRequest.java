package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Body of the plan actions that only need the version last read: {@code POST /plans/{id}/geocode},
 * {@code /optimize} (and {@code /publish}, {@code /cancel} later).
 */
public record PlanVersionRequest(@NotNull(message = "VERSION_REQUIRED") Long version) {}
