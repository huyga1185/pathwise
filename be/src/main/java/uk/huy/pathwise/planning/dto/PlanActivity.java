package uk.huy.pathwise.planning.dto;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Plans of a store for the ADMIN area (T55): how many, and when the store last worked on them.
 *
 * @param lastActivityAt the latest of a plan created or changed and a stop result reported by a driver (P-60); null
 *                       when the store has no plan
 */
public record PlanActivity(long plans, @Nullable Instant lastActivityAt) {}
