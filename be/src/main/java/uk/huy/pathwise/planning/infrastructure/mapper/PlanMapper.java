package uk.huy.pathwise.planning.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.huy.pathwise.planning.dto.response.DropReason;
import uk.huy.pathwise.planning.dto.response.LocationResponse;
import uk.huy.pathwise.planning.dto.response.PlanStopResponse;
import uk.huy.pathwise.planning.dto.response.PlanSummaryResponse;
import uk.huy.pathwise.planning.dto.response.TimeWindowResponse;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.RoutePlan;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface PlanMapper {
    @Mapping(target = "address", source = "stop.addressText")
    @Mapping(target = "timeWindow", expression = "java(toTimeWindow(stop))")
    @Mapping(target = "demands", expression = "java(java.util.Map.copyOf(stop.getDemands()))")
    PlanStopResponse toStopResponse(PlanStop stop, DropReason dropReason);

    PlanSummaryResponse toSummaryResponse(RoutePlan plan, long stopCount, long routeCount);

    default LocationResponse toDepot(RoutePlan plan) {
        return new LocationResponse(plan.getDepotAddress(), plan.getDepotLat(), plan.getDepotLon());
    }

    default LocationResponse toStart(PlanRoute route) {
        return new LocationResponse(route.getStartAddress(), route.getStartLat(), route.getStartLon());
    }

    default LocationResponse toEnd(PlanRoute route) {
        return new LocationResponse(route.getEndAddress(), route.getEndLat(), route.getEndLon());
    }

    default TimeWindowResponse toTimeWindow(PlanStop stop) {
        if (stop.getTimeWindowStart() == null) return null;
        return new TimeWindowResponse(stop.getTimeWindowStart(), stop.getTimeWindowEnd());
    }
}
