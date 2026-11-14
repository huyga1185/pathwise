package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.observability.DailyCounts;
import uk.huy.pathwise.planning.dto.DailyPlanStats;
import uk.huy.pathwise.planning.dto.PlanActivity;
import uk.huy.pathwise.planning.dto.PlanPeriodStats;
import uk.huy.pathwise.planning.model.StopStatus;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.TreeSet;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanStatsService implements PlanStatsQuery {
    private final RoutePlanRepository planRepository;

    @Override
    public Map<Long, PlanActivity> activityOf(Collection<Long> storeIds) {
        Map<Long, PlanActivity> activity = new HashMap<>();
        if (storeIds.isEmpty()) return activity;
        Map<Long, Instant> lastStopResult = new HashMap<>();
        for (Object[] row : planRepository.lastStopResultByStore(storeIds)) lastStopResult.put((Long) row[0], (Instant) row[1]);
        for (Object[] row : planRepository.summarizeByStore(storeIds)) {
            long storeId = (Long) row[0];
            activity.put(storeId, new PlanActivity((Long) row[1], latest((Instant) row[2], lastStopResult.get(storeId))));
        }
        return activity;
    }

    @Override
    public PlanPeriodStats statsSince(long storeId, Instant since) {
        Map<StopStatus, Long> results = new EnumMap<>(StopStatus.class);
        for (Object[] row : planRepository.countStopResultsSince(storeId, since)) results.put((StopStatus) row[0], (Long) row[1]);
        return new PlanPeriodStats(planRepository.countCreatedSince(storeId, since),
                planRepository.countPublishedSince(storeId, since),
                planRepository.countCompletedSince(storeId, since),
                results.getOrDefault(StopStatus.DELIVERED, 0L),
                results.getOrDefault(StopStatus.FAILED, 0L),
                results.getOrDefault(StopStatus.SKIPPED, 0L));
    }

    @Override
    public Map<LocalDate, DailyPlanStats> dailyStats(Instant from, Instant to, ZoneOffset offset) {
        int seconds = DailyCounts.offsetSeconds(offset);
        Map<LocalDate, Long> created = DailyCounts.byDay(planRepository.countCreatedByDay(from, to, seconds));
        Map<LocalDate, Long> published = DailyCounts.byDay(planRepository.countPublishedByDay(from, to, seconds));
        Map<LocalDate, Long> completed = DailyCounts.byDay(planRepository.countCompletedByDay(from, to, seconds));
        Map<LocalDate, Map<String, Long>> results = new HashMap<>();
        for (Object[] row : planRepository.countStopResultsByDay(from, to, seconds)) {
            results.computeIfAbsent(DailyCounts.day(row[0]), day -> new HashMap<>())
                    .put(String.valueOf(row[1]), ((Number) row[2]).longValue());
        }
        TreeSet<LocalDate> days = new TreeSet<>(created.keySet());
        days.addAll(published.keySet());
        days.addAll(completed.keySet());
        days.addAll(results.keySet());
        Map<LocalDate, DailyPlanStats> stats = new HashMap<>();
        for (LocalDate day : days) {
            Map<String, Long> stops = results.getOrDefault(day, Map.of());
            stats.put(day, new DailyPlanStats(created.getOrDefault(day, 0L), published.getOrDefault(day, 0L),
                    completed.getOrDefault(day, 0L), stops.getOrDefault(StopStatus.DELIVERED.name(), 0L),
                    stops.getOrDefault(StopStatus.FAILED.name(), 0L), stops.getOrDefault(StopStatus.SKIPPED.name(), 0L)));
        }
        return stats;
    }

    @Override
    public long countStoresPublishingSince(Instant since) {
        return planRepository.countStoresPublishingSince(since);
    }

    private static Instant latest(Instant a, Instant b) {
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
