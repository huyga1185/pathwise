package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.planning.dto.request.LocationBatchRequest;
import uk.huy.pathwise.planning.dto.request.LocationPointRequest;
import uk.huy.pathwise.planning.dto.request.UpdateLocationSharingRequest;
import uk.huy.pathwise.planning.dto.response.DriverLocationResponse;
import uk.huy.pathwise.planning.model.LocationPoint;
import uk.huy.pathwise.planning.model.LocationSharing;
import uk.huy.pathwise.planning.model.PlanLifecycle;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.RoutePlan;
import uk.huy.pathwise.planning.repository.PlanRouteRepository;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;
import uk.huy.pathwise.planning.service.location.LastKnownLocationSink;
import uk.huy.pathwise.planning.service.location.LocationSink;
import uk.huy.pathwise.planning.store.DriverLocationStore;
import uk.huy.pathwise.planning.store.LocationSharingStore;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Driver positions (FR-D04, ARCHITECTURE.md §9): the app sends batches of points (D-26) and reports whether it is
 * sharing (P-53); the dispatcher reads the last position and that state for each route of a running plan.
 */
@Service
@RequiredArgsConstructor
public class DriverLocationService {
    /** Points recorded further ahead of the server clock are left out: a phone clock running fast would otherwise
     * store a point that blocks every real point after it (P-53). */
    static final Duration MAX_AHEAD = Duration.ofMinutes(2);
    /** A working day: the app reports again whenever the state changes and whenever the route is opened. */
    static final Duration SHARING_TTL = Duration.ofHours(12);
    /** Plans a driver has seen: also CANCELLED, to answer PLAN_NOT_ACTIVE for one cancelled after publishing. */
    private static final Set<PlanStatus> SEEN = EnumSet.of(PlanStatus.PUBLISHED, PlanStatus.IN_PROGRESS,
            PlanStatus.COMPLETED, PlanStatus.CANCELLED);

    private final List<LocationSink> sinks;
    private final DriverLocationStore locationStore;
    private final LocationSharingStore sharingStore;
    private final PlanRouteRepository routeRepository;
    private final RoutePlanRepository planRepository;

    /**
     * Hands the usable points of the batch, oldest first, to every sink. The batch is accepted even if no point is
     * usable: the app has nothing to correct.
     */
    public void recordLocations(UserIdentity actor, LocationBatchRequest request) {
        if (request == null) throw new NullPointerException("Location batch request could not be null");
        long storeId = storeIdOf(actor);
        Instant latest = Instant.now().plus(MAX_AHEAD);
        List<LocationPoint> points = request.points().stream()
                .filter(point -> !point.recordedAt().isAfter(latest))
                .map(DriverLocationService::toPoint)
                .sorted(Comparator.comparing(LocationPoint::recordedAt))
                .toList();
        if (points.isEmpty()) return;
        sinks.forEach(sink -> sink.accept(storeId, actor.id(), points));
    }

    /**
     * @throws AppException {@code NOT_FOUND} for a route the caller cannot see (another driver's or store's, without
     *                      stops, never published), as {@code GET /driver/routes/{id}}; {@code PLAN_NOT_ACTIVE} if its
     *                      plan is COMPLETED or CANCELLED
     */
    @Transactional(readOnly = true)
    public void updateSharing(UserIdentity actor, UpdateLocationSharingRequest request) {
        if (request == null) throw new NullPointerException("Update location sharing request could not be null");
        long storeId = storeIdOf(actor);
        PlanRoute route = routeRepository.findDriverRoute(request.routeId(), storeId, actor.id(), SEEN)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        RoutePlan plan = route.getPlan();
        boolean hasStops = plan.getStops().stream()
                .anyMatch(stop -> stop.getRoute() != null && stop.getRoute().getId().equals(route.getId()));
        if (!hasStops || plan.getPublishedAt() == null) throw new AppException(ErrorCode.NOT_FOUND);
        if (!PlanLifecycle.isActive(plan.getStatus())) throw new AppException(ErrorCode.PLAN_NOT_ACTIVE);
        sharingStore.save(storeId, actor.id(), new LocationSharing(route.getId(), request.state(), Instant.now()), SHARING_TTL);
    }

    /**
     * @return one element per route with stops, in the order of {@code PlanDetail.routes}; empty unless the plan is
     * PUBLISHED or IN_PROGRESS
     * @throws AppException {@code NOT_FOUND} for a plan of another store
     */
    @Transactional(readOnly = true)
    public List<DriverLocationResponse> planLocations(UserIdentity actor, long planId) {
        long storeId = storeIdOf(actor);
        RoutePlan plan = planRepository.findByIdAndStoreId(planId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (!PlanLifecycle.isActive(plan.getStatus())) return List.of();
        Set<Long> routed = plan.getStops().stream()
                .map(PlanStop::getRoute)
                .filter(Objects::nonNull)
                .map(PlanRoute::getId)
                .collect(Collectors.toSet());
        Instant oldest = Instant.now().minus(LastKnownLocationSink.MAX_AGE);
        return plan.getRoutes().stream()
                .filter(route -> routed.contains(route.getId()))
                .sorted(Comparator.comparing(PlanRoute::getId))
                .map(route -> {
                    long driverId = route.getDriverId();
                    DriverLocationResponse.DriverPosition location = locationStore.find(storeId, driverId)
                            .filter(point -> !point.recordedAt().isBefore(oldest))
                            .map(point -> new DriverLocationResponse.DriverPosition(point.lat(), point.lon(),
                                    point.accuracyMeters(), point.headingDegrees(), point.speedMps(), point.recordedAt()))
                            .orElse(null);
                    DriverLocationResponse.DriverSharing sharing = sharingStore.find(storeId, driverId)
                            .filter(state -> state.routeId() == route.getId())
                            .map(state -> new DriverLocationResponse.DriverSharing(state.state(), state.updatedAt()))
                            .orElse(null);
                    return new DriverLocationResponse(route.getId(), driverId, location, sharing);
                })
                .toList();
    }

    private static LocationPoint toPoint(LocationPointRequest point) {
        return new LocationPoint(point.lat(), point.lon(), point.accuracyMeters(), point.headingDegrees(),
                point.speedMps(), point.recordedAt());
    }

    /**
     * @throws AppException {@code NOT_FOUND} for an actor without a store
     */
    private static long storeIdOf(UserIdentity actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        return actor.storeId();
    }
}
