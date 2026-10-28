package uk.huy.pathwise.fleet.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.fleet.dto.FleetVehicle;
import uk.huy.pathwise.fleet.dto.request.CreateVehicleRequest;
import uk.huy.pathwise.fleet.dto.request.UpdateVehicleRequest;
import uk.huy.pathwise.fleet.dto.response.VehicleResponse;
import uk.huy.pathwise.fleet.infrastructure.mapper.VehicleMapper;
import uk.huy.pathwise.fleet.model.Vehicle;
import uk.huy.pathwise.fleet.repository.VehicleRepository;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Vehicles of the caller's store (FR-V01). The store always comes from the access token; a vehicle of another store
 * is reported as not found (BR-01). Names are unique within a store; another store may use the same name.
 */
@Service
@RequiredArgsConstructor
public class VehicleService implements VehicleQuery {
    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    /**
     * @param active only active or only inactive vehicles, if set
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> listVehicles(UserIdentity actor, Boolean active) {
        long storeId = storeIdOf(actor);
        List<Vehicle> vehicles = active == null
                ? vehicleRepository.findAllByStoreIdOrderByNameAscIdAsc(storeId)
                : vehicleRepository.findAllByStoreIdAndActiveOrderByNameAscIdAsc(storeId, active);
        return vehicles.stream().map(vehicleMapper::toVehicleResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(UserIdentity actor, long vehicleId) {
        return vehicleMapper.toVehicleResponse(getVehicleOf(actor, vehicleId));
    }

    /**
     * @throws AppException {@code VEHICLE_NAME_EXISTED} if the store already has a vehicle with this name
     */
    @Transactional
    public VehicleResponse createVehicle(UserIdentity actor, CreateVehicleRequest request) {
        if (request == null) throw new NullPointerException("Create vehicle request could not be null");
        long storeId = storeIdOf(actor);
        String name = request.name().trim();
        // A concurrent insert of the same name still hits the unique key and answers 409 CONFLICT.
        if (vehicleRepository.existsByStoreIdAndName(storeId, name)) throw new AppException(ErrorCode.VEHICLE_NAME_EXISTED);
        Vehicle vehicle = Vehicle.builder()
                .storeId(storeId)
                .name(name)
                .plateNumber(blankToNull(request.plateNumber()))
                .capacity(request.capacity())
                .capacityUnit(request.capacityUnit())
                .active(true)
                .build();
        return vehicleMapper.toVehicleResponse(vehicleRepository.save(vehicle));
    }

    /**
     * @throws AppException {@code NOT_FOUND} if the vehicle is not in the caller's store;
     *                      {@code VEHICLE_NAME_EXISTED} if another vehicle of the store has this name
     */
    @Transactional
    public VehicleResponse updateVehicle(UserIdentity actor, long vehicleId, UpdateVehicleRequest request) {
        if (request == null) throw new NullPointerException("Update vehicle request could not be null");
        Vehicle vehicle = getVehicleOf(actor, vehicleId);
        String name = request.name().trim();
        if (vehicleRepository.existsByStoreIdAndNameAndIdNot(vehicle.getStoreId(), name, vehicleId))
            throw new AppException(ErrorCode.VEHICLE_NAME_EXISTED);
        vehicle.setName(name);
        vehicle.setPlateNumber(blankToNull(request.plateNumber()));
        vehicle.setCapacity(request.capacity());
        vehicle.setCapacityUnit(request.capacityUnit());
        vehicle.setActive(request.active());
        return vehicleMapper.toVehicleResponse(vehicleRepository.save(vehicle));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FleetVehicle> findActive(long storeId, long vehicleId) {
        return vehicleRepository.findByIdAndStoreId(vehicleId, storeId)
                .filter(Vehicle::isActive)
                .map(vehicleMapper::toFleetVehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FleetVehicle> listActive(long storeId) {
        return vehicleRepository.findAllByStoreIdAndActiveOrderByNameAscIdAsc(storeId, true).stream()
                .map(vehicleMapper::toFleetVehicle)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<FleetVehicle> findAll(long storeId, Collection<Long> vehicleIds) {
        if (vehicleIds.isEmpty()) return List.of();
        return vehicleRepository.findAllByStoreIdAndIdIn(storeId, vehicleIds).stream()
                .map(vehicleMapper::toFleetVehicle)
                .toList();
    }

    private Vehicle getVehicleOf(UserIdentity actor, long vehicleId) {
        return vehicleRepository.findByIdAndStoreId(vehicleId, storeIdOf(actor))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    /**
     * @throws AppException {@code NOT_FOUND} for an actor without a store (ADMIN)
     */
    private static long storeIdOf(UserIdentity actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        return actor.storeId();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
