package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.ProcessingStatus;
import uk.huy.pathwise.planning.model.RoutePlan;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;
import uk.huy.pathwise.routing.model.Coordinate;
import uk.huy.pathwise.routing.solver.VrpSolution;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The two transactions of the optimize job (ARCHITECTURE.md §8.3): read the input, then write the result if the plan
 * has not changed. Both use {@code REQUIRES_NEW} (see {@link uk.huy.pathwise.job.service.JobHandler}).
 */
@Service
@RequiredArgsConstructor
class PlanOptimizationService {
    private final RoutePlanRepository planRepository;

    /**
     * @return the input of the plan, or empty if it does not exist
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<OptimizationInput> loadInput(long storeId, long planId) {
        return planRepository.findByIdAndStoreId(planId, storeId).map(PlanOptimizationService::toInput);
    }

    /**
     * Replaces the optimization result and puts the plan back to IDLE in the same transaction, so the plan version
     * moves once for the job (P-30). Dropped stops get no route (P-13).
     *
     * @throws AppException {@code PLAN_MODIFIED} if the plan is no longer at {@code input.version()}; nothing is
     *                      written
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveResult(long storeId, long planId, long jobId, OptimizationInput input, VrpSolution solution) {
        RoutePlan plan = planRepository.findByIdAndStoreIdForUpdate(planId, storeId)
                .filter(candidate -> candidate.getVersion() == input.version())
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_MODIFIED));
        Map<Long, PlanRoute> routes = plan.getRoutes().stream().collect(Collectors.toMap(PlanRoute::getId, Function.identity()));
        Map<Long, PlanStop> stops = plan.getStops().stream().collect(Collectors.toMap(PlanStop::getId, Function.identity()));
        ZonedDateTime midnight = input.planDate().atStartOfDay(ZoneId.of(input.timezone()));

        for (PlanStop stop : plan.getStops()) {
            stop.setRoute(null);
            stop.setSequence(null);
            stop.setPlannedArrival(null);
        }
        int totalDistance = 0;
        int totalDuration = 0;
        for (VrpSolution.VrpRoute result : solution.routes()) {
            PlanRoute route = require(routes, input.routes().get(result.vehicleIndex()).id());
            if (result.visits().isEmpty()) {
                // An unused vehicle does not drive.
                route.setPlannedDistanceMeters(0);
                route.setPlannedDurationSeconds(0);
                route.setPlannedStart(null);
                route.setPlannedEnd(null);
                continue;
            }
            route.setPlannedDistanceMeters(result.distanceMeters());
            route.setPlannedDurationSeconds(result.durationSeconds());
            route.setPlannedStart(toInstant(midnight, result.startSeconds()));
            route.setPlannedEnd(toInstant(midnight, result.endSeconds()));
            for (int i = 0; i < result.visits().size(); i++) {
                VrpSolution.VrpVisit visit = result.visits().get(i);
                PlanStop stop = require(stops, input.stops().get(visit.stopIndex()).id());
                stop.setRoute(route);
                stop.setSequence(i + 1);
                stop.setPlannedArrival(toInstant(midnight, visit.arrivalSeconds()));
            }
            totalDistance += result.distanceMeters();
            totalDuration += result.durationSeconds();
        }
        plan.setOptimizedAt(Instant.now());
        plan.setTotalDistanceMeters(totalDistance);
        plan.setTotalDurationSeconds(totalDuration);
        if (Objects.equals(plan.getActiveJobId(), jobId)) {
            plan.setProcessingStatus(ProcessingStatus.IDLE);
            plan.setActiveJobId(null);
        }
    }

    private static OptimizationInput toInput(RoutePlan plan) {
        List<OptimizationInput.Route> routes = plan.getRoutes().stream()
                .sorted(Comparator.comparing(PlanRoute::getId))
                .map(route -> new OptimizationInput.Route(route.getId(),
                        new Coordinate(route.getStartLat(), route.getStartLon()),
                        new Coordinate(route.getEndLat(), route.getEndLon()),
                        route.getVehicleCapacityUnit(),
                        route.getVehicleCapacity(),
                        route.getShiftStart(),
                        route.getShiftEnd(),
                        route.getMaxDistanceMeters()))
                .toList();
        List<OptimizationInput.Stop> stops = plan.getStops().stream()
                .sorted(Comparator.comparing(PlanStop::getId))
                .map(stop -> new OptimizationInput.Stop(stop.getId(),
                        new Coordinate(stop.getLat(), stop.getLon()),
                        Map.copyOf(stop.getDemands()),
                        stop.getServiceTimeSeconds(),
                        stop.getTimeWindowStart(),
                        stop.getTimeWindowEnd()))
                .toList();
        return new OptimizationInput(plan.getVersion(), plan.getPlanDate(), plan.getTimezone(), routes, stops);
    }

    // Seconds count from local midnight; Asia/Ho_Chi_Minh has no daylight saving, so this is the wall clock time.
    private static Instant toInstant(ZonedDateTime midnight, int seconds) {
        return midnight.plusSeconds(seconds).toInstant();
    }

    /**
     * @throws AppException {@code PLAN_MODIFIED} if the route or stop read by the job is gone
     */
    private static <T> T require(Map<Long, T> byId, long id) {
        T value = byId.get(id);
        if (value == null) throw new AppException(ErrorCode.PLAN_MODIFIED);
        return value;
    }
}
