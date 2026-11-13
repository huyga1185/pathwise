package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.DetailedAppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.FieldErrorDetail;
import uk.huy.pathwise.planning.dto.request.ReplaceRoutesRequest;
import uk.huy.pathwise.planning.dto.response.PlanDetailResponse;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.ProcessingStatus;
import uk.huy.pathwise.planning.model.RoutePlan;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;
import uk.huy.pathwise.routing.matrix.MatrixProvider;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.model.Coordinate;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manual route editing after optimization (FR-P08, D-24, T28, P-58): the dispatcher moves stops between routes,
 * reorders them or takes them off; arrivals and distances are recomputed with the travel matrix, without the solver.
 * The plan is read and checked in one transaction, the matrix asked outside any transaction (ARCHITECTURE.md §8.3),
 * and the result written in another transaction that checks the version again. The edit is saved even if it breaks
 * constraints: {@code PlanDetail.violations} says which.
 */
@Service
@RequiredArgsConstructor
public class ManualRouteService {
    private final RoutePlanRepository planRepository;
    private final MatrixProvider matrixProvider;
    private final PlanDetailAssembler planDetailAssembler;
    private final TransactionTemplate transactionTemplate;

    /**
     * @throws AppException {@code NOT_FOUND}, {@code PLAN_NOT_EDITABLE} (not a DRAFT), {@code PLAN_BUSY},
     *                      {@code PLAN_MODIFIED} (also if the plan changed while the matrix was asked),
     *                      {@code PLAN_NOT_OPTIMIZED}, {@code VALIDATION_ERROR} with the field of each wrong id
     * @throws uk.huy.pathwise.core.exception.ExternalServiceException {@code ROUTING_PROVIDER_ERROR} if the matrix
     *         fails; nothing is written
     */
    public PlanDetailResponse replaceRoutes(UserIdentity actor, long planId, ReplaceRoutesRequest request) {
        if (request == null) throw new NullPointerException("Replace routes request could not be null");
        long storeId = storeIdOf(actor);
        Edit edit = transactionTemplate.execute(status -> read(storeId, planId, request));
        TravelMatrix matrix = edit.nodes().isEmpty() ? null : matrixProvider.matrix(edit.nodes());
        Map<Long, ManualSchedule.RouteTimes> times = new HashMap<>();
        for (RouteInput route : edit.routes()) {
            if (!route.stops().isEmpty()) times.put(route.id(), schedule(route, matrix));
        }
        return transactionTemplate.execute(status -> write(storeId, planId, edit, times));
    }

    /**
     * What the edit needs outside the transaction: the routes in request order with their stops, and the matrix nodes.
     *
     * @param unassignedStopIds the stops of the plan on no route after the edit
     */
    private record Edit(long version, LocalDate planDate, String timezone, List<RouteInput> routes,
                        Set<Long> unassignedStopIds, List<Coordinate> nodes) {}

    /**
     * @param startNode index of the start point in {@link Edit#nodes()}
     */
    private record RouteInput(long id, int startNode, int endNode, LocalTime shiftStart, List<StopInput> stops) {}

    private record StopInput(long id, int node, int serviceSeconds, LocalTime windowStart) {}

    private Edit read(long storeId, long planId, ReplaceRoutesRequest request) {
        RoutePlan plan = planRepository.findByIdAndStoreId(planId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        checkEditable(plan, request.version());
        if (plan.getOptimizedAt() == null) throw new AppException(ErrorCode.PLAN_NOT_OPTIMIZED);
        validate(plan, request);

        Map<Long, PlanRoute> routes = plan.getRoutes().stream().collect(Collectors.toMap(PlanRoute::getId, Function.identity()));
        Map<Long, PlanStop> stops = plan.getStops().stream().collect(Collectors.toMap(PlanStop::getId, Function.identity()));
        // Same point, same node: the depot is shared by the routes, a stop may sit where another one is.
        Map<Coordinate, Integer> nodes = new LinkedHashMap<>();
        List<RouteInput> inputs = new ArrayList<>();
        for (ReplaceRoutesRequest.RouteStops requested : request.routes()) {
            PlanRoute route = routes.get(requested.routeId());
            List<StopInput> visits = new ArrayList<>();
            int startNode = -1;
            int endNode = -1;
            if (!requested.stopIds().isEmpty()) {
                startNode = node(nodes, new Coordinate(route.getStartLat(), route.getStartLon()));
                endNode = node(nodes, new Coordinate(route.getEndLat(), route.getEndLon()));
                for (Long stopId : requested.stopIds()) {
                    PlanStop stop = stops.get(stopId);
                    visits.add(new StopInput(stopId, node(nodes, new Coordinate(stop.getLat(), stop.getLon())),
                            stop.getServiceTimeSeconds(), stop.getTimeWindowStart()));
                }
            }
            inputs.add(new RouteInput(route.getId(), startNode, endNode, route.getShiftStart(), List.copyOf(visits)));
        }
        return new Edit(plan.getVersion(), plan.getPlanDate(), plan.getTimezone(), List.copyOf(inputs),
                Set.copyOf(request.unassignedStopIds()), List.copyOf(nodes.keySet()));
    }

    private static int node(Map<Coordinate, Integer> nodes, Coordinate point) {
        return nodes.computeIfAbsent(point, key -> nodes.size());
    }

    /**
     * Every route of the plan once; every stop of the plan once, on a route or unassigned; only stops with coordinates
     * on a route. The same error for an unknown id, another plan's and a repeated one: nothing about other plans leaks.
     */
    private static void validate(RoutePlan plan, ReplaceRoutesRequest request) {
        Set<Long> routeIds = plan.getRoutes().stream().map(PlanRoute::getId).collect(Collectors.toSet());
        Map<Long, PlanStop> stops = plan.getStops().stream().collect(Collectors.toMap(PlanStop::getId, Function.identity()));
        List<FieldErrorDetail> errors = new ArrayList<>();
        Set<Long> seenRoutes = new HashSet<>();
        Set<Long> seenStops = new HashSet<>();
        for (int i = 0; i < request.routes().size(); i++) {
            ReplaceRoutesRequest.RouteStops route = request.routes().get(i);
            if (route == null) {
                errors.add(new FieldErrorDetail("routes[" + i + "]", "INVALID_ROUTE_ID"));
                continue;
            }
            if (!routeIds.contains(route.routeId()) || !seenRoutes.add(route.routeId()))
                errors.add(new FieldErrorDetail("routes[" + i + "].routeId", "INVALID_ROUTE_ID"));
            for (int j = 0; j < route.stopIds().size(); j++) {
                Long stopId = route.stopIds().get(j);
                String field = "routes[" + i + "].stopIds[" + j + "]";
                if (stopId == null || !stops.containsKey(stopId) || !seenStops.add(stopId)) {
                    errors.add(new FieldErrorDetail(field, "INVALID_STOP_ID"));
                } else if (stops.get(stopId).getLat() == null || stops.get(stopId).getLon() == null) {
                    errors.add(new FieldErrorDetail(field, "STOP_NOT_GEOCODED"));
                }
            }
        }
        for (int j = 0; j < request.unassignedStopIds().size(); j++) {
            Long stopId = request.unassignedStopIds().get(j);
            if (stopId == null || !stops.containsKey(stopId) || !seenStops.add(stopId))
                errors.add(new FieldErrorDetail("unassignedStopIds[" + j + "]", "INVALID_STOP_ID"));
        }
        if (!seenRoutes.containsAll(routeIds)) errors.add(new FieldErrorDetail("routes", "ROUTES_INCOMPLETE"));
        if (!seenStops.containsAll(stops.keySet())) errors.add(new FieldErrorDetail("stops", "STOPS_INCOMPLETE"));
        if (!errors.isEmpty()) throw new DetailedAppException(ErrorCode.VALIDATION_ERROR, errors);
    }

    private static ManualSchedule.RouteTimes schedule(RouteInput route, TravelMatrix matrix) {
        int legs = route.stops().size() + 1;
        int[] durations = new int[legs];
        int[] distances = new int[legs];
        int from = route.startNode();
        for (int i = 0; i < legs; i++) {
            int to = i < route.stops().size() ? route.stops().get(i).node() : route.endNode();
            durations[i] = matrix.durationSeconds()[from][to];
            distances[i] = matrix.distanceMeters()[from][to];
            from = to;
        }
        List<ManualSchedule.Visit> visits = route.stops().stream()
                .map(stop -> new ManualSchedule.Visit(stop.serviceSeconds(),
                        stop.windowStart() == null ? null : stop.windowStart().toSecondOfDay()))
                .toList();
        return ManualSchedule.compute(route.shiftStart().toSecondOfDay(), visits, durations, distances);
    }

    private PlanDetailResponse write(long storeId, long planId, Edit edit, Map<Long, ManualSchedule.RouteTimes> times) {
        RoutePlan plan = planRepository.findByIdAndStoreIdForUpdate(planId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        // Changed while the matrix was asked (another edit, a new optimization): the edit was made on what it replaced.
        checkEditable(plan, edit.version());
        if (plan.getOptimizedAt() == null) throw new AppException(ErrorCode.PLAN_NOT_OPTIMIZED);

        Map<Long, PlanRoute> routes = plan.getRoutes().stream().collect(Collectors.toMap(PlanRoute::getId, Function.identity()));
        Map<Long, PlanStop> stops = plan.getStops().stream().collect(Collectors.toMap(PlanStop::getId, Function.identity()));
        ZonedDateTime midnight = edit.planDate().atStartOfDay(ZoneId.of(edit.timezone()));
        for (PlanStop stop : plan.getStops()) {
            // Taken off by the dispatcher now or before; a stop the solver left out stays INFEASIBLE.
            boolean manual = edit.unassignedStopIds().contains(stop.getId())
                    && (stop.getRoute() != null || stop.isManuallyUnassigned());
            stop.setManuallyUnassigned(manual);
            stop.setRoute(null);
            stop.setSequence(null);
            stop.setPlannedArrival(null);
        }
        int totalDistance = 0;
        int totalDuration = 0;
        for (RouteInput input : edit.routes()) {
            PlanRoute route = require(routes, input.id());
            ManualSchedule.RouteTimes routeTimes = times.get(input.id());
            if (routeTimes == null) {
                // A route left without stops does not drive, as an unused vehicle of the optimizer.
                route.setPlannedDistanceMeters(0);
                route.setPlannedDurationSeconds(0);
                route.setPlannedStart(null);
                route.setPlannedEnd(null);
                continue;
            }
            route.setPlannedDistanceMeters(routeTimes.distanceMeters());
            route.setPlannedDurationSeconds(routeTimes.durationSeconds());
            route.setPlannedStart(toInstant(midnight, routeTimes.startSeconds()));
            route.setPlannedEnd(toInstant(midnight, routeTimes.endSeconds()));
            for (int i = 0; i < input.stops().size(); i++) {
                PlanStop stop = require(stops, input.stops().get(i).id());
                stop.setRoute(route);
                stop.setSequence(i + 1);
                stop.setPlannedArrival(toInstant(midnight, routeTimes.serviceStarts().get(i)));
            }
            totalDistance += routeTimes.distanceMeters();
            totalDuration += routeTimes.durationSeconds();
        }
        plan.setTotalDistanceMeters(totalDistance);
        plan.setTotalDurationSeconds(totalDuration);
        // Routes and stops are outside the plan's optimistic lock: make the plan row dirty so the version moves once.
        plan.setUpdatedAt(Instant.now());
        planRepository.flush();
        return planDetailAssembler.toDetail(plan);
    }

    private static void checkEditable(RoutePlan plan, Long version) {
        if (plan.getStatus() != PlanStatus.DRAFT) throw new AppException(ErrorCode.PLAN_NOT_EDITABLE);
        if (plan.getProcessingStatus() != ProcessingStatus.IDLE) throw new AppException(ErrorCode.PLAN_BUSY);
        if (version == null || version != plan.getVersion()) throw new AppException(ErrorCode.PLAN_MODIFIED);
    }

    // Seconds count from local midnight; Asia/Ho_Chi_Minh has no daylight saving, so this is the wall clock time.
    private static Instant toInstant(ZonedDateTime midnight, int seconds) {
        return midnight.plusSeconds(seconds).toInstant();
    }

    /**
     * @throws AppException {@code PLAN_MODIFIED} if a route or stop read before the matrix is gone
     */
    private static <T> T require(Map<Long, T> byId, long id) {
        T value = byId.get(id);
        if (value == null) throw new AppException(ErrorCode.PLAN_MODIFIED);
        return value;
    }

    /**
     * @throws AppException {@code NOT_FOUND} for an actor without a store (ADMIN)
     */
    private static long storeIdOf(UserIdentity actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        return actor.storeId();
    }
}
