package uk.huy.pathwise.job.dto;

/**
 * Optimizations started on one day (T60), and how many failed.
 */
public record DailyOptimizations(long total, long failed) {}
