package uk.huy.pathwise.fleet.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import uk.huy.pathwise.fleet.dto.FleetVehicle;
import uk.huy.pathwise.fleet.dto.response.VehicleResponse;
import uk.huy.pathwise.fleet.model.Vehicle;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface VehicleMapper {
    VehicleResponse toVehicleResponse(Vehicle vehicle);

    FleetVehicle toFleetVehicle(Vehicle vehicle);
}
