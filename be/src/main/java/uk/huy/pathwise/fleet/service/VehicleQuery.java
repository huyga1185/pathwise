package uk.huy.pathwise.fleet.service;

import uk.huy.pathwise.fleet.dto.FleetVehicle;

import java.util.Collection;
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

    /**
     * For showing vehicles already referenced elsewhere (for example by a plan), so a vehicle deactivated since then
     * is still returned.
     *
     * @return the vehicles among {@code vehicleIds} that belong to {@code storeId}, active or not; ids of other
     * stores' vehicles are left out
     */
    List<FleetVehicle> findAll(long storeId, Collection<Long> vehicleIds);
}
