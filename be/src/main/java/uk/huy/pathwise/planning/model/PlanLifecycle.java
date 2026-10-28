package uk.huy.pathwise.planning.model;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Status rules of plans and stops (ARCHITECTURE.md §8.1, BR-06, BR-10, BR-11), shared by the dispatcher API (T26) and
 * the driver API (T31). Every transition goes one way: a finished stop or plan never goes back (P-34).
 */
public final class PlanLifecycle {
    private static final Map<PlanStatus, Set<PlanStatus>> PLAN_TRANSITIONS = new EnumMap<>(PlanStatus.class);
    private static final Map<StopStatus, Set<StopStatus>> STOP_TRANSITIONS = new EnumMap<>(StopStatus.class);

    static {
        PLAN_TRANSITIONS.put(PlanStatus.DRAFT, EnumSet.of(PlanStatus.PUBLISHED, PlanStatus.CANCELLED));
        // Straight to COMPLETED when the first update finishes every stop, for example a one-stop plan.
        PLAN_TRANSITIONS.put(PlanStatus.PUBLISHED, EnumSet.of(PlanStatus.IN_PROGRESS, PlanStatus.COMPLETED, PlanStatus.CANCELLED));
        PLAN_TRANSITIONS.put(PlanStatus.IN_PROGRESS, EnumSet.of(PlanStatus.COMPLETED));
        PLAN_TRANSITIONS.put(PlanStatus.COMPLETED, EnumSet.noneOf(PlanStatus.class));
        PLAN_TRANSITIONS.put(PlanStatus.CANCELLED, EnumSet.noneOf(PlanStatus.class));
        STOP_TRANSITIONS.put(StopStatus.PENDING, EnumSet.of(StopStatus.DELIVERED, StopStatus.FAILED, StopStatus.SKIPPED));
        STOP_TRANSITIONS.put(StopStatus.DELIVERED, EnumSet.noneOf(StopStatus.class));
        STOP_TRANSITIONS.put(StopStatus.FAILED, EnumSet.noneOf(StopStatus.class));
        STOP_TRANSITIONS.put(StopStatus.SKIPPED, EnumSet.noneOf(StopStatus.class));
    }

    private PlanLifecycle() {}

    public static boolean canMove(PlanStatus from, PlanStatus to) {
        return PLAN_TRANSITIONS.get(from).contains(to);
    }

    public static boolean canMove(StopStatus from, StopStatus to) {
        return STOP_TRANSITIONS.get(from).contains(to);
    }

    /**
     * Drivers see and update a plan only while it is running (BR-10).
     */
    public static boolean isActive(PlanStatus status) {
        return status == PlanStatus.PUBLISHED || status == PlanStatus.IN_PROGRESS;
    }

    /**
     * Where a running plan stands given the statuses of its assigned stops (BR-11): IN_PROGRESS once a stop has left
     * PENDING, COMPLETED once every one has finished. Stops without a route do not count.
     *
     * @return the new status, or {@code current} if nothing changes
     */
    public static PlanStatus progress(PlanStatus current, List<StopStatus> assignedStops) {
        if (!isActive(current) || assignedStops.isEmpty()) return current;
        boolean started = assignedStops.stream().anyMatch(status -> status != StopStatus.PENDING);
        boolean finished = assignedStops.stream().noneMatch(status -> status == StopStatus.PENDING);
        if (finished) return PlanStatus.COMPLETED;
        if (started) return PlanStatus.IN_PROGRESS;
        return current;
    }
}
