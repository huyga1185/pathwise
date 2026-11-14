package uk.huy.pathwise.admin.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Business numbers of the platform (T60, FR-A16), from the database. Days are in {@code timezone}; {@code days} has
 * every day from {@code from} to {@code to}, with zeros. Cached about a minute: {@code computedAt} says when.
 */
public record BusinessMetricsResponse(LocalDate from,
                                      LocalDate to,
                                      String timezone,
                                      Instant computedAt,
                                      Now now,
                                      Totals totals,
                                      List<Day> days) {
    /**
     * At {@code computedAt}, whatever the range.
     *
     * @param activeStores7d  stores that published a plan in the last 7 days
     * @param activeStores30d same, 30 days
     * @param lockedStores    stores READ_ONLY
     */
    public record Now(long stores, long activeStores7d, long activeStores30d, long lockedStores,
                      long admins, long owners, long dispatchers, long drivers) {}

    /**
     * Over the range. Rates are 0–1, rounded to 4 digits; null when there is nothing to divide.
     *
     * @param deliveredRate         stopsDelivered / (delivered + failed + skipped)
     * @param optimizationFailureRate optimizationsFailed / optimizations
     */
    public record Totals(long newStores, long plansCreated, long plansPublished, long plansCompleted,
                         long stopsDelivered, long stopsFailed, long stopsSkipped,
                         @Nullable Double deliveredRate, @Nullable Double failedRate, @Nullable Double skippedRate,
                         long optimizations, long optimizationsFailed, @Nullable Double optimizationFailureRate) {}

    public record Day(LocalDate date, long newStores, long plansCreated, long plansPublished, long plansCompleted,
                      long stopsDelivered, long stopsFailed, long stopsSkipped,
                      long optimizations, long optimizationsFailed) {}
}
