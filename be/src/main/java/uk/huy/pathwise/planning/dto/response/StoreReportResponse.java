package uk.huy.pathwise.planning.dto.response;

import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code GET /reports/summary} (T63, P-67, P-68): the store's plans whose {@code planDate} is in {@code [from, to]}.
 * Stop counts, distances and drivers only cover plans given to drivers (PUBLISHED, IN_PROGRESS, COMPLETED); a
 * cancelled plan is not delivery work.
 *
 * @param plans                 every plan of the period by status, cancelled ones included
 * @param plannedDistanceMeters sum of the planned distance of the routes that have stops
 * @param days                  every day of the period, in order, also days without plans
 * @param drivers               drivers with a route that has stops, by name
 * @param failureReasons        the 10 most frequent reasons of FAILED stops, most frequent first
 */
public record StoreReportResponse(LocalDate from,
                                  LocalDate to,
                                  ReportPlanCounts plans,
                                  ReportStopCounts stops,
                                  long plannedDistanceMeters,
                                  List<ReportDay> days,
                                  List<ReportDriver> drivers,
                                  List<ReportFailureReason> failureReasons) {
    public record ReportPlanCounts(long total, long draft, long published, long inProgress, long completed, long cancelled) {}

    /**
     * @param deliveredRate delivered / (delivered + failed + skipped), null while no stop has a result
     */
    public record ReportStopCounts(long total, long delivered, long failed, long skipped, long pending,
                             @Nullable Double deliveredRate) {}

    /**
     * @param plans plans of the day given to drivers
     */
    public record ReportDay(LocalDate date, long plans, long delivered, long failed, long skipped, long pending) {}

    /**
     * @param firstResultAt the first stop result the driver reported in the period, null before any
     * @param lastResultAt  the last one
     */
    public record ReportDriver(long driverId,
                         String fullName,
                         long routes,
                         long delivered,
                         long failed,
                         long skipped,
                         long pending,
                         long plannedDistanceMeters,
                         @Nullable Instant firstResultAt,
                         @Nullable Instant lastResultAt) {}

    /**
     * @param reason as the first report of it was written, trimmed
     */
    public record ReportFailureReason(String reason, long count) {}
}
