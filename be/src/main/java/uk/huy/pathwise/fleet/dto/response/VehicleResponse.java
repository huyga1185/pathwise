package uk.huy.pathwise.fleet.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.fleet.model.CapacityUnit;

public record VehicleResponse(long id, String name, @Nullable String plateNumber, int capacity, CapacityUnit capacityUnit, boolean active) {}
