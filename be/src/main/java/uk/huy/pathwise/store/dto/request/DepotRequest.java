package uk.huy.pathwise.store.dto.request;

import jakarta.validation.constraints.*;

/**
 * Default depot of the store (D-20). Coordinates are required: the depot is never geocoded.
 */
public record DepotRequest(@Size(max = 500, message = "INVALID_ADDRESS_LENGTH") String address,
                           @NotNull(message = "LATITUDE_REQUIRED") @DecimalMin(value = "-90", message = "INVALID_LATITUDE") @DecimalMax(value = "90", message = "INVALID_LATITUDE") Double lat,
                           @NotNull(message = "LONGITUDE_REQUIRED") @DecimalMin(value = "-180", message = "INVALID_LONGITUDE") @DecimalMax(value = "180", message = "INVALID_LONGITUDE") Double lon) {}
