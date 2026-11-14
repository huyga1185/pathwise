package uk.huy.pathwise.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.observability.DailyCounts;
import uk.huy.pathwise.job.dto.DailyOptimizations;
import uk.huy.pathwise.job.model.JobStatus;
import uk.huy.pathwise.job.repository.BackgroundJobRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobStatsService implements JobStatsQuery {
    private final BackgroundJobRepository jobRepository;

    @Override
    public Map<LocalDate, DailyOptimizations> optimizationsByDay(Instant from, Instant to, ZoneOffset offset) {
        Map<LocalDate, DailyOptimizations> days = new HashMap<>();
        for (Object[] row : jobRepository.countOptimizationsByDay(from, to, DailyCounts.offsetSeconds(offset))) {
            days.put(DailyCounts.day(row[0]), new DailyOptimizations(((Number) row[1]).longValue(), ((Number) row[2]).longValue()));
        }
        return days;
    }

    @Override
    public long countUnfinished() {
        return jobRepository.countByStatusIn(List.of(JobStatus.QUEUED, JobStatus.RUNNING));
    }

    @Override
    public long countFailedSince(Instant since) {
        return jobRepository.countByStatusAndFinishedAtGreaterThanEqual(JobStatus.FAILED, since);
    }
}
