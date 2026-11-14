package uk.huy.pathwise.job.service;

import uk.huy.pathwise.job.dto.DailyOptimizations;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

/**
 * Job counts of the whole platform, for the {@code admin} module (T60, P-64). Finished jobs are kept 100 days (T53).
 */
public interface JobStatsQuery {
    /**
     * @return optimizations created in {@code [from, to)} by local day at {@code offset}; days without any are absent
     */
    Map<LocalDate, DailyOptimizations> optimizationsByDay(Instant from, Instant to, ZoneOffset offset);

    /**
     * @return jobs QUEUED or RUNNING now
     */
    long countUnfinished();

    /**
     * @return jobs that ended FAILED since {@code since}
     */
    long countFailedSince(Instant since);
}
