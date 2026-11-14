package uk.huy.pathwise.planning.service;

import uk.huy.pathwise.planning.dto.response.ViolationResponse;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStop;
import uk.huy.pathwise.planning.model.RoutePlan;
import uk.huy.pathwise.planning.model.ViolationCode;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * The constraints the stored routes of a plan break (API_SPEC §7, T28, P-58), computed at each read from what the
 * routes hold: load, arrivals, return time and distance. Nothing is stored, so it never disagrees with the routes.
 * A plan without an optimization result has none.
 */
final class PlanViolations {
    private PlanViolations() {}

    /**
     * @param stopsByRoute the stops of each route, by route id
     * @return by route (id order), then by stop sequence
     */
    static List<ViolationResponse> of(RoutePlan plan, Map<Long, List<PlanStop>> stopsByRoute) {
        if (plan.getOptimizedAt() == null) return List.of();
        ZoneId zone = ZoneId.of(plan.getTimezone());
        List<ViolationResponse> violations = new ArrayList<>();
        plan.getRoutes().stream().sorted(Comparator.comparing(PlanRoute::getId)).forEach(route -> {
            List<PlanStop> stops = stopsByRoute.getOrDefault(route.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(PlanStop::getSequence))
                    .toList();
            if (stops.isEmpty()) return;
            int load = PlanDetailAssembler.load(route.getVehicleCapacityUnit(), stops);
            if (load > route.getVehicleCapacity())
                violations.add(new ViolationResponse(ViolationCode.CAPACITY_EXCEEDED, route.getId(), null,
                        load - route.getVehicleCapacity()));
            for (PlanStop stop : stops) {
                if (stop.getTimeWindowEnd() == null || stop.getPlannedArrival() == null) continue;
                long late = secondsAfter(stop.getPlannedArrival(), plan, stop.getTimeWindowEnd(), zone);
                if (late > 0)
                    violations.add(new ViolationResponse(ViolationCode.TIME_WINDOW_MISSED, route.getId(), stop.getId(),
                            (int) late));
            }
            if (route.getPlannedEnd() != null) {
                long over = secondsAfter(route.getPlannedEnd(), plan, route.getShiftEnd(), zone);
                if (over > 0)
                    violations.add(new ViolationResponse(ViolationCode.SHIFT_EXCEEDED, route.getId(), null, (int) over));
            }
            Integer max = route.getMaxDistanceMeters();
            Integer distance = route.getPlannedDistanceMeters();
            if (max != null && distance != null && distance > max)
                violations.add(new ViolationResponse(ViolationCode.MAX_DISTANCE_EXCEEDED, route.getId(), null,
                        distance - max));
        });
        return List.copyOf(violations);
    }

    /**
     * @return seconds from {@code limit} on the plan date, in the plan timezone, to {@code instant}; negative if before
     */
    private static long secondsAfter(Instant instant, RoutePlan plan, LocalTime limit, ZoneId zone) {
        Instant end = plan.getPlanDate().atTime(limit).atZone(zone).toInstant();
        return Duration.between(end, instant).toSeconds();
    }
}
