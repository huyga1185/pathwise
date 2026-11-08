package uk.huy.pathwise.planning.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import uk.huy.pathwise.core.validation.FieldErrors;
import uk.huy.pathwise.core.validation.SelfValidated;
import uk.huy.pathwise.core.validation.SelfValidating;

import java.time.LocalTime;

/**
 * A driver with a vehicle and a shift (BR-09). The route starts and ends at the plan depot in the MVP (D-20).
 *
 * @param driverId a member of the store who may drive: DRIVER, or OWNER/DISPATCHER with {@code canDrive} (D-21)
 */
@SelfValidated
public record AssignmentRequest(@NotNull(message = "DRIVER_REQUIRED") Long driverId,
                                @NotNull(message = "VEHICLE_REQUIRED") Long vehicleId,
                                @NotNull(message = "SHIFT_START_REQUIRED") @JsonFormat(pattern = "HH:mm") LocalTime shiftStart,
                                @NotNull(message = "SHIFT_END_REQUIRED") @JsonFormat(pattern = "HH:mm") LocalTime shiftEnd,
                                @Positive(message = "INVALID_MAX_DISTANCE") Integer maxDistanceMeters) implements SelfValidating {
    @Override
    public void validate(FieldErrors errors) {
        if (shiftStart != null && shiftEnd != null && !shiftStart.isBefore(shiftEnd)) errors.reject("shiftEnd", "INVALID_SHIFT");
    }
}
