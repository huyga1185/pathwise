package uk.huy.pathwise.store.dto.request;

import jakarta.validation.constraints.*;

/**
 * Replaces the store settings ({@code PUT /store}). The depot coordinates are optional but come as a pair.
 */
public record UpdateStoreRequest(@NotBlank(message = "STORE_NAME_REQUIRED") @Size(max = 150, message = "INVALID_STORE_NAME_LENGTH") String name,
                                 @Size(max = 500, message = "INVALID_DEPOT_ADDRESS_LENGTH") String depotAddress,
                                 @DecimalMin(value = "-90", message = "INVALID_LATITUDE") @DecimalMax(value = "90", message = "INVALID_LATITUDE") Double depotLat,
                                 @DecimalMin(value = "-180", message = "INVALID_LONGITUDE") @DecimalMax(value = "180", message = "INVALID_LONGITUDE") Double depotLon,
                                 @NotBlank(message = "TIMEZONE_REQUIRED") @Size(max = 50, message = "INVALID_TIMEZONE") String timezone) {
    @AssertTrue(message = "DEPOT_COORDINATES_INCOMPLETE")
    public boolean isDepotCoordinatesComplete() {
        return (depotLat == null) == (depotLon == null);
    }
}
