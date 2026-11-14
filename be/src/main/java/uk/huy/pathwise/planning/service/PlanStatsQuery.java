package uk.huy.pathwise.planning.service;

import uk.huy.pathwise.planning.dto.DailyPlanStats;
import uk.huy.pathwise.planning.dto.PlanActivity;
import uk.huy.pathwise.planning.dto.PlanPeriodStats;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;

/**
 * Aggregates of plans across stores, for the {@code admin} module (T55, P-60). Not tenant-scoped: only ADMIN
 * endpoints may use it.
 */
public interface PlanStatsQuery {
    /**
     * @return the plan activity of each of {@code storeIds} that has plans
     */
    Map<Long, PlanActivity> activityOf(Collection<Long> storeIds);

    /**
     * @return the counts of the store from {@code since} (inclusive) until now
     */
    PlanPeriodStats statsSince(long storeId, Instant since);

    /**
     * @return the whole platform's counts in {@code [from, to)} by local day at {@code offset} (T60); days without
     * anything are absent
     */
    Map<LocalDate, DailyPlanStats> dailyStats(Instant from, Instant to, ZoneOffset offset);

    /**
     * @return stores that published a plan since {@code since} (T60)
     */
    long countStoresPublishingSince(Instant since);
}
