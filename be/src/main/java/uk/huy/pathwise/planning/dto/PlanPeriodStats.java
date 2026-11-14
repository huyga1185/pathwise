package uk.huy.pathwise.planning.dto;

/**
 * Counts of a store since a moment, for the ADMIN area (T55): plans by event time, and stop results reported by
 * drivers in that time. Numbers only, no plan content (P-59).
 */
public record PlanPeriodStats(long plansCreated,
                              long plansPublished,
                              long plansCompleted,
                              long stopsDelivered,
                              long stopsFailed,
                              long stopsSkipped) {}
