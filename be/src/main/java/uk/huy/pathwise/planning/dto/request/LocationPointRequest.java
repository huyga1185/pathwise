package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/**
 * @param headingDegrees clockwise from north, 0 (inclusive) to 360 (exclusive)
 * @param recordedAt     when the phone took the position
 */
public record LocationPointRequest(
        @NotNull(message = "LATITUDE_REQUIRED") @DecimalMin(value = "-90", message = "INVALID_LATITUDE")
        @DecimalMax(value = "90", message = "INVALID_LATITUDE") Double lat,
        @NotNull(message = "LONGITUDE_REQUIRED") @DecimalMin(value = "-180", message = "INVALID_LONGITUDE")
        @DecimalMax(value = "180", message = "INVALID_LONGITUDE") Double lon,
        @PositiveOrZero(message = "INVALID_ACCURACY") Double accuracyMeters,
        @DecimalMin(value = "0", message = "INVALID_HEADING")
        @DecimalMax(value = "360", inclusive = false, message = "INVALID_HEADING") Double headingDegrees,
        @PositiveOrZero(message = "INVALID_SPEED") Double speedMps,
        @NotNull(message = "RECORDED_AT_REQUIRED") Instant recordedAt) {}
