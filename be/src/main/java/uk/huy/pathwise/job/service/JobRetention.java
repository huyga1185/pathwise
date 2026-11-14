package uk.huy.pathwise.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.housekeeping.RetentionTask;
import uk.huy.pathwise.job.repository.BackgroundJobRepository;

import java.time.Instant;

/**
 * Housekeeping of {@code background_jobs} (T53): jobs finished before the cutoff (30 days by default). After that
 * {@code GET /jobs/{id}} answers 404, which the clients never ask for so late.
 */
@Component
@RequiredArgsConstructor
class JobRetention implements RetentionTask {
    private final BackgroundJobRepository jobRepository;

    @Override
    public String name() {
        return "jobs";
    }

    @Override
    @Transactional
    public int purge(Instant cutoff) {
        return jobRepository.deleteFinishedBefore(cutoff);
    }
}
