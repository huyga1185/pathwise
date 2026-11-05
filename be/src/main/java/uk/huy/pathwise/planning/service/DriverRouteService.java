package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.planning.dto.request.UpdateStopStatusRequest;
import uk.huy.pathwise.planning.dto.response.DriverRouteDetailResponse;
import uk.huy.pathwise.planning.dto.response.DriverRouteSummaryResponse;
import uk.huy.pathwise.planning.dto.response.DriverStopUpdateResponse;
import uk.huy.pathwise.planning.infrastructure.mapper.PlanMapper;
import uk.huy.pathwise.planning.model.PlanLifecycle;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.RoutePlan;
import uk.huy.pathwise.planning.model.StopStatus;
import uk.huy.pathwise.planning.repository.PlanRouteRepository;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.store.service.StoreQuery;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Routes of the calling driver (FR-D01, API_SPEC §8): a DRIVER, or an OWNER/DISPATCHER with {@code canDrive} (D-21).
 * Only the caller's own routes that have stops (P-34), in plans that were published: PUBLISHED, IN_PROGRESS or
 * COMPLETED. Anything else is reported as not found, like another store's data (BR-01).
 */
@Service
@RequiredArgsConstructor
public class DriverRouteService {
    static final Set<PlanStatus> VISIBLE = EnumSet.of(PlanStatus.PUBLISHED, PlanStatus.IN_PROGRESS, PlanStatus.COMPLETED);

    private final PlanRouteRepository routeRepository;
    private final RoutePlanRepository planRepository;
    private final StoreQuery storeQuery;
    private final PlanMapper planMapper;
    private final TransactionTemplate transactionTemplate;

    /**
     * @param planDate the day to list; today in the store timezone if null
     * @return the routes of that day, by planned start
     */
    @Transactional(readOnly = true)
    public List<DriverRouteSummaryResponse> listRoutes(UserIdentity actor, LocalDate planDate) {
        long storeId = storeIdOf(actor);
        LocalDate day = planDate != null ? planDate : today(storeId);
        return routeRepository.findDriverRoutes(storeId, actor.id(), day, VISIBLE).stream()
                .map(route -> {
                    List<PlanStop> stops = stopsOf(route);
                    return stops.isEmpty() ? null : toSummary(route, stops);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(DriverRouteSummaryResponse::plannedStart,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(DriverRouteSummaryResponse::routeId))
                .toList();
    }

    /**
     * @throws AppException {@code NOT_FOUND} for a route that is not the caller's, has no stop, or is in a plan that
     *                      is not published (DRAFT, CANCELLED)
     */
    @Transactional(readOnly = true)
    public DriverRouteDetailResponse getRoute(UserIdentity actor, long routeId) {
        PlanRoute route = routeRepository.findDriverRoute(routeId, storeIdOf(actor), actor.id(), VISIBLE)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        List<PlanStop> stops = stopsOf(route);
        if (stops.isEmpty()) throw new AppException(ErrorCode.NOT_FOUND);
        return new DriverRouteDetailResponse(route.getId(),
                route.getPlan().getId(),
                route.getPlan().getName(),
                route.getPlan().getPlanDate(),
                route.getPlan().getTimezone(),
                PlanLifecycle.routeStatus(statuses(stops)),
                planMapper.toStart(route),
                planMapper.toEnd(route),
                stops.stream().map(planMapper::toDriverStop).toList());
    }

    /**
     * The driver reports a stop of their route as DELIVERED, FAILED or SKIPPED (FR-D03, BR-10), one way only (P-34).
     * The plan moves to IN_PROGRESS with the first finished stop and to COMPLETED with the last one (BR-11), without
     * changing its {@code version} (P-32). Sending the status the stop already has changes nothing and succeeds, so the
     * app can retry a request whose response was lost, even after the plan has completed.
     * <p>
     * The plan row is locked while updating: two drivers finishing the last stops of their routes at the same time
     * both see each other's stop, and a dispatcher cancelling at the same time is either before or after.
     *
     * @throws AppException {@code NOT_FOUND} for a route that is not the caller's or was never published, or a stop not
     *                      on that route; {@code INVALID_STOP_TRANSITION} for PENDING or a stop already finished with
     *                      another status; {@code PLAN_NOT_ACTIVE} if the plan is COMPLETED or CANCELLED;
     *                      {@code FAILURE_REASON_REQUIRED} for FAILED without a reason
     */
    public DriverStopUpdateResponse updateStopStatus(UserIdentity actor, long routeId, long stopId, UpdateStopStatusRequest request) {
        if (request == null) throw new NullPointerException("Update stop status request could not be null");
        long storeId = storeIdOf(actor);
        // Find the plan in its own transaction: the locking read must be the first read of the updating transaction
        // (MariaDB snapshot isolation, F-44). A route never moves to another plan.
        long planId = routeRepository.findDriverRoutePlanId(routeId, storeId, actor.id())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return transactionTemplate.execute(status -> updateStopStatusLocked(actor.id(), storeId, planId, routeId, stopId, request));
    }

    private DriverStopUpdateResponse updateStopStatusLocked(long driverId, long storeId, long planId, long routeId, long stopId,
                                                            UpdateStopStatusRequest request) {
        RoutePlan plan = planRepository.findByIdAndStoreIdForUpdate(planId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        // Never shown to the driver: a draft, or a plan cancelled before it was published.
        if (plan.getStatus() == PlanStatus.DRAFT || plan.getPublishedAt() == null) throw new AppException(ErrorCode.NOT_FOUND);
        PlanRoute route = plan.getRoutes().stream()
                // The draft may have been replaced, with new routes, between the two reads.
                .filter(candidate -> candidate.getId() == routeId && candidate.getDriverId() == driverId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        List<PlanStop> stops = stopsOf(route);
        PlanStop stop = stops.stream()
                .filter(candidate -> candidate.getId() == stopId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        StopStatus target = request.status();
        if (target == StopStatus.PENDING) throw new AppException(ErrorCode.INVALID_STOP_TRANSITION);
        if (stop.getStatus() != target) {
            if (!PlanLifecycle.isActive(plan.getStatus())) throw new AppException(ErrorCode.PLAN_NOT_ACTIVE);
            if (!PlanLifecycle.canMove(stop.getStatus(), target)) throw new AppException(ErrorCode.INVALID_STOP_TRANSITION);
            String failureReason = request.failureReason() == null ? null : request.failureReason().strip();
            if (target == StopStatus.FAILED && (failureReason == null || failureReason.isEmpty())) {
                throw new AppException(ErrorCode.FAILURE_REASON_REQUIRED);
            }
            stop.setStatus(target);
            stop.setFailureReason(target == StopStatus.FAILED ? failureReason : null);
            stop.setStatusUpdatedAt(Instant.now());
            stop.setStatusUpdatedBy(driverId);
        }

        PlanStatus planStatus = PlanLifecycle.progress(plan.getStatus(), plan.getStops().stream()
                .filter(candidate -> candidate.getRoute() != null)
                .map(PlanStop::getStatus)
                .toList());
        if (planStatus != plan.getStatus()) {
            planRepository.updateStatus(planId, planStatus, planStatus == PlanStatus.COMPLETED ? Instant.now() : null);
        }
        return new DriverStopUpdateResponse(planMapper.toDriverStop(stop), PlanLifecycle.routeStatus(statuses(stops)), planStatus);
    }

    private DriverRouteSummaryResponse toSummary(PlanRoute route, List<PlanStop> stops) {
        int completed = (int) stops.stream().filter(stop -> stop.getStatus() != StopStatus.PENDING).count();
        return new DriverRouteSummaryResponse(route.getId(),
                route.getPlan().getId(),
                route.getPlan().getName(),
                route.getPlan().getPlanDate(),
                route.getPlan().getTimezone(),
                PlanLifecycle.routeStatus(statuses(stops)),
                stops.size(),
                completed,
                route.getPlannedStart());
    }

    /**
     * @return the stops of the route, in visiting order
     */
    private static List<PlanStop> stopsOf(PlanRoute route) {
        return route.getPlan().getStops().stream()
                .filter(stop -> stop.getRoute() != null && stop.getRoute().getId().equals(route.getId()))
                .sorted(Comparator.comparing(PlanStop::getSequence))
                .toList();
    }

    private static List<StopStatus> statuses(List<PlanStop> stops) {
        return stops.stream().map(PlanStop::getStatus).toList();
    }

    private LocalDate today(long storeId) {
        String timezone = storeQuery.findStore(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND))
                .timezone();
        return LocalDate.now(ZoneId.of(timezone));
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
