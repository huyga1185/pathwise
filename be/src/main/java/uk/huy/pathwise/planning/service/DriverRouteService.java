package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.planning.dto.response.DriverRouteDetailResponse;
import uk.huy.pathwise.planning.dto.response.DriverRouteSummaryResponse;
import uk.huy.pathwise.planning.infrastructure.mapper.PlanMapper;
import uk.huy.pathwise.planning.model.PlanLifecycle;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.StopStatus;
import uk.huy.pathwise.planning.repository.PlanRouteRepository;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.store.service.StoreQuery;

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
    private final StoreQuery storeQuery;
    private final PlanMapper planMapper;

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
                route.getPlan().getPlanDate(),
                PlanLifecycle.routeStatus(statuses(stops)),
                planMapper.toStart(route),
                planMapper.toEnd(route),
                stops.stream().map(planMapper::toDriverStop).toList());
    }

    private DriverRouteSummaryResponse toSummary(PlanRoute route, List<PlanStop> stops) {
        int completed = (int) stops.stream().filter(stop -> stop.getStatus() != StopStatus.PENDING).count();
        return new DriverRouteSummaryResponse(route.getId(),
                route.getPlan().getId(),
                route.getPlan().getName(),
                route.getPlan().getPlanDate(),
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
