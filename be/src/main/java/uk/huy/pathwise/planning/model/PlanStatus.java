package uk.huy.pathwise.planning.model;

/**
 * Lifecycle of a plan (ARCHITECTURE.md §8.1). Only a DRAFT can be edited (BR-06).
 */
public enum PlanStatus {
    DRAFT,
    PUBLISHED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
