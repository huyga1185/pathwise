package uk.huy.pathwise.fleet.dto;

import uk.huy.pathwise.fleet.model.CapacityUnit;

/**
 * A vehicle as other modules see it (ARCHITECTURE.md §2.1).
 *
 * @param capacityUnit what {@code capacity} counts (D-36)
 */
public record FleetVehicle(long id,
                           long storeId,
                           String name,
                           String plateNumber,
                           int capacity,
                           CapacityUnit capacityUnit,
                           boolean active) {}
