package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.ratelimit.RateLimitDecision;
import uk.huy.pathwise.core.ratelimit.RateLimitedException;
import uk.huy.pathwise.core.ratelimit.RateLimiter;
import uk.huy.pathwise.core.statestore.StateStoreException;
import uk.huy.pathwise.planning.dto.response.DirectionsResponse;
import uk.huy.pathwise.planning.model.PlanLifecycle;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.RoutePlan;
import uk.huy.pathwise.planning.model.StopStatus;
import uk.huy.pathwise.planning.repository.PlanRouteRepository;
import uk.huy.pathwise.routing.directions.Directions;
import uk.huy.pathwise.routing.directions.DirectionsProvider;
import uk.huy.pathwise.routing.model.Coordinate;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Turn-by-turn directions for the driver app (FR-D05, T33, P-57): from the driver's current position to a stop of
 * their route. The route is found as {@code GET /driver/routes/{id}} finds it; the provider is called after the
 * read transaction has ended (ARCHITECTURE.md §8.5: no external call inside a DB transaction). Not cached: the start
 * is wherever the driver is.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverDirectionsService {
    static final String RATE_LIMIT_RULE = "directions-user";
    /** Plans a driver has seen: also CANCELLED, to answer PLAN_NOT_ACTIVE for one cancelled after publishing. */
    private static final Set<PlanStatus> SEEN = EnumSet.of(PlanStatus.PUBLISHED, PlanStatus.IN_PROGRESS,
            PlanStatus.COMPLETED, PlanStatus.CANCELLED);

    private final PlanRouteRepository routeRepository;
    private final DirectionsProvider directionsProvider;
    private final RateLimiter rateLimiter;
    private final TransactionTemplate transactionTemplate;

    /**
     * @param toStopId the stop to drive to; null for the first PENDING stop, or the end of the route if none is left
     * @throws AppException {@code NOT_FOUND} for a route the caller cannot see (as {@code GET /driver/routes/{id}}) or a
     *                      {@code toStopId} not on it; {@code PLAN_NOT_ACTIVE} if the plan is COMPLETED or CANCELLED;
     *                      {@code RATE_LIMITED} past the limit of the caller
     * @throws uk.huy.pathwise.core.exception.ExternalServiceException {@code ROUTING_PROVIDER_ERROR} if the provider
     *         fails or finds no road
     */
    public DirectionsResponse directions(UserIdentity actor, long routeId, double lat, double lon, Long toStopId) {
        long storeId = storeIdOf(actor);
        enforceRateLimit(actor.id());
        Target target = transactionTemplate.execute(status -> target(storeId, actor.id(), routeId, toStopId));
        Directions directions = directionsProvider.directions(new Coordinate(lat, lon), target.point());
        return new DirectionsResponse(
                new DirectionsResponse.DirectionsDestination(target.stopId(), target.point().lat(), target.point().lon()),
                directions.distanceMeters(),
                directions.durationSeconds(),
                directions.polyline(),
                directions.steps().stream()
                        .map(step -> new DirectionsResponse.DirectionsInstruction(step.maneuver(), step.exitNumber(), step.roadName(),
                                step.distanceMeters(), step.durationSeconds(),
                                new DirectionsResponse.DirectionsPoint(step.location().lat(), step.location().lon())))
                        .toList());
    }

    /**
     * @param stopId null for the end of the route
     */
    private record Target(Long stopId, Coordinate point) {}

    private Target target(long storeId, long driverId, long routeId, Long toStopId) {
        PlanRoute route = routeRepository.findDriverRoute(routeId, storeId, driverId, SEEN)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        RoutePlan plan = route.getPlan();
        List<PlanStop> stops = plan.getStops().stream()
                .filter(stop -> stop.getRoute() != null && stop.getRoute().getId().equals(route.getId()))
                .sorted(Comparator.comparing(PlanStop::getSequence))
                .toList();
        // Never shown to the driver: no stop, or a plan cancelled before it was published.
        if (stops.isEmpty() || plan.getPublishedAt() == null) throw new AppException(ErrorCode.NOT_FOUND);
        if (!PlanLifecycle.isActive(plan.getStatus())) throw new AppException(ErrorCode.PLAN_NOT_ACTIVE);

        if (toStopId != null) {
            PlanStop stop = stops.stream()
                    .filter(candidate -> candidate.getId().equals(toStopId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
            return toTarget(stop);
        }
        return stops.stream()
                .filter(stop -> stop.getStatus() == StopStatus.PENDING)
                .findFirst()
                .map(DriverDirectionsService::toTarget)
                .orElseGet(() -> new Target(null, new Coordinate(route.getEndLat(), route.getEndLon())));
    }

    private static Target toTarget(PlanStop stop) {
        // A stop on a route was optimized, so it has coordinates.
        return new Target(stop.getId(), new Coordinate(stop.getLat(), stop.getLon()));
    }

    private void enforceRateLimit(long driverId) {
        RateLimitDecision decision;
        try {
            decision = rateLimiter.tryAcquire(RATE_LIMIT_RULE, String.valueOf(driverId));
        } catch (StateStoreException e) {
            log.error("Rate limit could not be checked, refusing the request", e);
            throw new AppException(ErrorCode.SERVICE_UNAVAILABLE);
        }
        switch (decision) {
            case RateLimitDecision.Allowed allowed -> {}
            case RateLimitDecision.Limited limited -> throw new RateLimitedException(limited.retryAfter());
        }
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
