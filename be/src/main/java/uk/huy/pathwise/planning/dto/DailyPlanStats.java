package uk.huy.pathwise.planning.dto;

/**
 * What the whole platform did on one day (T60): plans by event, stop results reported by drivers.
 */
public record DailyPlanStats(long plansCreated,
                             long plansPublished,
                             long plansCompleted,
                             long stopsDelivered,
                             long stopsFailed,
                             long stopsSkipped) {
    public static final DailyPlanStats NONE = new DailyPlanStats(0, 0, 0, 0, 0, 0);
}
