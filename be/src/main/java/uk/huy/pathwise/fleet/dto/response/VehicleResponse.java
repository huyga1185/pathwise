package uk.huy.pathwise.fleet.dto.response;

import uk.huy.pathwise.fleet.model.CapacityUnit;

public record VehicleResponse(long id, String name, String plateNumber, int capacity, CapacityUnit capacityUnit, boolean active) {}
