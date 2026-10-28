package uk.huy.pathwise.fleet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import uk.huy.pathwise.fleet.model.CapacityUnit;

/**
 * Replaces every field of the vehicle ({@code PUT}); a missing {@code plateNumber} clears it.
 */
public record UpdateVehicleRequest(@NotBlank(message = "VEHICLE_NAME_REQUIRED") @Size(max = 100, message = "INVALID_VEHICLE_NAME_LENGTH") String name,
                                   @Size(max = 20, message = "INVALID_PLATE_NUMBER_LENGTH") String plateNumber,
                                   @NotNull(message = "CAPACITY_REQUIRED") @Positive(message = "INVALID_CAPACITY") Integer capacity,
                                   @NotNull(message = "CAPACITY_UNIT_REQUIRED") CapacityUnit capacityUnit,
                                   @NotNull(message = "ACTIVE_REQUIRED") Boolean active) {}
