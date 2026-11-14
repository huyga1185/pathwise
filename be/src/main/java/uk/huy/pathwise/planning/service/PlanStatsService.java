package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.planning.dto.PlanActivity;
import uk.huy.pathwise.planning.dto.PlanPeriodStats;
import uk.huy.pathwise.planning.model.StopStatus;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;

import java.time.Instant;
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

    private static Instant latest(Instant a, Instant b) {
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
