package uk.huy.pathwise.fleet.service;

import uk.huy.pathwise.fleet.dto.FleetVehicle;

import java.util.List;
import java.util.Optional;

/**
 * Public API of the {@code fleet} module, for {@code planning} to check the vehicles of a plan (BR-07).
 */
public interface VehicleQuery {
    /**
     * @return the vehicle if it belongs to {@code storeId} and is active; empty otherwise, including for a vehicle of
     * another store
     */
    Optional<FleetVehicle> findActive(long storeId, long vehicleId);

    /**
     * @return the active vehicles of the store, by name
     */
    List<FleetVehicle> listActive(long storeId);
}
