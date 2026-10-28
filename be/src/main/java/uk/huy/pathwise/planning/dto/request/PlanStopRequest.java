package uk.huy.pathwise.planning.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import uk.huy.pathwise.core.phonenumber.ValidPhoneNumber;
import uk.huy.pathwise.fleet.model.CapacityUnit;

import java.util.Map;

/**
 * A stop of a plan draft (BR-08). It needs an address, coordinates, or both; with coordinates it is not geocoded
 * (D-18).
 *
 * @param id                 id of an existing stop of this plan ({@code PUT} only), to keep its geocoding result
 *                           while its address and coordinates do not change
 * @param recipientPhone     optional Vietnamese number, stored as E.164
 * @param demands            per capacity unit (D-36), missing units are 0; {@code ORDER} is not accepted because
 *                           every stop counts as one order. An unknown unit is 400 {@code MALFORMED_REQUEST}.
 * @param serviceTimeSeconds optional, 0 by default
 */
public record PlanStopRequest(Long id,
                              @NotBlank(message = "STOP_LABEL_REQUIRED") @Size(max = 150, message = "INVALID_STOP_LABEL_LENGTH") String label,
                              @Size(max = 100, message = "INVALID_RECIPIENT_NAME_LENGTH") String recipientName,
                              @ValidPhoneNumber String recipientPhone,
                              @Size(max = 500, message = "INVALID_ADDRESS_LENGTH") String address,
                              @DecimalMin(value = "-90", message = "INVALID_LATITUDE") @DecimalMax(value = "90", message = "INVALID_LATITUDE") Double lat,
                              @DecimalMin(value = "-180", message = "INVALID_LONGITUDE") @DecimalMax(value = "180", message = "INVALID_LONGITUDE") Double lon,
                              Map<CapacityUnit, @NotNull(message = "INVALID_DEMAND") @PositiveOrZero(message = "INVALID_DEMAND") Integer> demands,
                              @PositiveOrZero(message = "INVALID_SERVICE_TIME") @Max(value = 86_400, message = "INVALID_SERVICE_TIME") Integer serviceTimeSeconds,
                              @Valid TimeWindowRequest timeWindow,
                              @Size(max = 500, message = "INVALID_NOTE_LENGTH") String note) {
    @AssertTrue(message = "COORDINATES_INCOMPLETE")
    public boolean isCoordinatesComplete() {
        return (lat == null) == (lon == null);
    }

    @AssertTrue(message = "ADDRESS_OR_COORDINATES_REQUIRED")
    public boolean isLocated() {
        return (address != null && !address.isBlank()) || (lat != null && lon != null);
    }

    @AssertTrue(message = "ORDER_DEMAND_NOT_ALLOWED")
    public boolean isWithoutOrderDemand() {
        return demands == null || !demands.containsKey(CapacityUnit.ORDER);
    }
}
