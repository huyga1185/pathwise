package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.fleet.dto.FleetVehicle;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.fleet.service.VehicleQuery;
import uk.huy.pathwise.job.dto.JobResponse;
import uk.huy.pathwise.job.service.JobService;
import uk.huy.pathwise.planning.dto.response.*;
import uk.huy.pathwise.planning.infrastructure.mapper.PlanMapper;
import uk.huy.pathwise.planning.model.GeocodeStatus;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.RoutePlan;
import uk.huy.pathwise.store.dto.StoreMember;
import uk.huy.pathwise.store.service.StoreMemberQuery;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds {@code PlanDetail} (API_SPEC §7): the plan with its routes, their stops in order, and the stops without a
 * route. Driver and vehicle names are read from their modules at read time, so a renamed vehicle shows its new name;
 * capacities are the snapshot of the assignment. Call it inside the transaction that loaded the plan.
 */
@Component
@RequiredArgsConstructor
class PlanDetailAssembler {
    private final StoreMemberQuery storeMemberQuery;
    private final VehicleQuery vehicleQuery;
    private final PlanMapper planMapper;
    private final JobService jobService;

    PlanDetailResponse toDetail(RoutePlan plan) {
        long storeId = plan.getStoreId();
        List<PlanRoute> routes = plan.getRoutes();
        Map<Long, StoreMember> drivers = storeMemberQuery
                .findMembers(storeId, routes.stream().map(PlanRoute::getDriverId).toList()).stream()
                .collect(Collectors.toMap(StoreMember::id, Function.identity()));
        Map<Long, FleetVehicle> vehicles = vehicleQuery
                .findAll(storeId, routes.stream().map(PlanRoute::getVehicleId).toList()).stream()
                .collect(Collectors.toMap(FleetVehicle::id, Function.identity()));
        Map<Long, List<PlanStop>> stopsByRoute = plan.getStops().stream()
                .filter(stop -> stop.getRoute() != null)
                .collect(Collectors.groupingBy(stop -> stop.getRoute().getId()));
        JobResponse activeJob = plan.getActiveJobId() == null ? null
                : jobService.findJob(storeId, plan.getActiveJobId()).orElse(null);
        DropReason dropReason = plan.getOptimizedAt() == null ? DropReason.NOT_OPTIMIZED : DropReason.INFEASIBLE;

        List<PlanRouteResponse> routeResponses = routes.stream()
                .sorted(Comparator.comparing(PlanRoute::getId))
                .map(route -> toRoute(plan, route, drivers.get(route.getDriverId()), vehicles.get(route.getVehicleId()),
                        stopsByRoute.getOrDefault(route.getId(), List.of())))
                .toList();
        List<PlanStopResponse> unassigned = plan.getStops().stream()
                .filter(stop -> stop.getRoute() == null)
                .sorted(Comparator.comparing(PlanStop::getId))
                .map(stop -> planMapper.toStopResponse(stop, dropReason))
                .toList();
        return new PlanDetailResponse(plan.getId(),
                plan.getName(),
                plan.getPlanDate(),
                plan.getStatus(),
                plan.getVersion(),
                plan.getProcessingStatus(),
                activeJob,
                geocodingSummary(plan.getStops()),
                planMapper.toDepot(plan),
                plan.getTimezone(),
                plan.getOptimizedAt(),
                plan.getTotalDistanceMeters(),
                plan.getTotalDurationSeconds(),
                routeResponses,
                unassigned);
    }

    private PlanRouteResponse toRoute(RoutePlan plan, PlanRoute route, StoreMember driver, FleetVehicle vehicle, List<PlanStop> stops) {
        List<PlanStop> ordered = stops.stream().sorted(Comparator.comparing(PlanStop::getSequence)).toList();
        return new PlanRouteResponse(route.getId(),
                // Missing only if the user or vehicle left the store's data, which the schema does not allow.
                new PlanRouteResponse.Driver(route.getDriverId(),
                        driver == null ? null : driver.fullName(),
                        driver == null ? null : driver.role()),
                new PlanRouteResponse.Vehicle(route.getVehicleId(),
                        vehicle == null ? null : vehicle.name(),
                        route.getVehicleCapacity(),
                        route.getVehicleCapacityUnit()),
                route.getShiftStart(),
                route.getShiftEnd(),
                route.getMaxDistanceMeters(),
                planMapper.toStart(route),
                planMapper.toEnd(route),
                route.getPlannedDistanceMeters(),
                route.getPlannedDurationSeconds(),
                route.getPlannedStart(),
                route.getPlannedEnd(),
                plan.getOptimizedAt() == null ? null : load(route.getVehicleCapacityUnit(), ordered),
                ordered.stream().map(stop -> planMapper.toStopResponse(stop, null)).toList());
    }

    /**
     * Load in the vehicle's unit (D-36): the number of stops for {@code ORDER}, the sum of that unit's demand otherwise.
     */
    static int load(CapacityUnit unit, List<PlanStop> stops) {
        if (unit == CapacityUnit.ORDER) return stops.size();
        return stops.stream().mapToInt(stop -> stop.getDemands().getOrDefault(unit, 0)).sum();
    }

    private static GeocodingSummaryResponse geocodingSummary(List<PlanStop> stops) {
        Map<GeocodeStatus, Long> counts = stops.stream()
                .collect(Collectors.groupingBy(PlanStop::getGeocodeStatus, Collectors.counting()));
        return new GeocodingSummaryResponse(stops.size(),
                counts.getOrDefault(GeocodeStatus.RESOLVED, 0L).intValue(),
                counts.getOrDefault(GeocodeStatus.PROVIDED, 0L).intValue(),
                counts.getOrDefault(GeocodeStatus.PENDING, 0L).intValue(),
                counts.getOrDefault(GeocodeStatus.FAILED, 0L).intValue());
    }
}
