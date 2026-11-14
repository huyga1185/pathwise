package uk.huy.pathwise.admin.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * What a store did from {@code since} until now (T55).
 *
 * @param plansCreated   plans created in the period
 * @param plansPublished plans published in the period
 * @param plansCompleted plans completed in the period
 * @param stopsDelivered stop results reported in the period, by result
 * @param deliveredRate  stopsDelivered / (delivered + failed + skipped), 0 to 1; null when no result was reported
 */
public record StorePeriodStatsResponse(Instant since,
                                       long plansCreated,
                                       long plansPublished,
                                       long plansCompleted,
                                       long stopsDelivered,
                                       long stopsFailed,
                                       long stopsSkipped,
                                       @Nullable Double deliveredRate) {}
