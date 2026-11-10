package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * A batch of positions (D-26): the app sends what it collected since the last successful request, including the
 * points gathered while offline.
 */
public record LocationBatchRequest(
        @NotEmpty(message = "POINTS_REQUIRED") @Size(max = 100, message = "TOO_MANY_POINTS")
        List<@Valid @NotNull(message = "POINT_REQUIRED") LocationPointRequest> points) {}
