package uk.huy.pathwise.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import uk.huy.pathwise.job.dto.JobRef;

/**
 * At startup, fails the jobs left QUEUED or RUNNING by the previous run and lets their handlers release the targets
 * (plan back to IDLE). Valid only with a single instance (ARCHITECTURE.md §8.4).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobRecovery {
    private final JobStateService jobStateService;
    private final JobExecutor jobExecutor;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobs() {
        List<JobRef> interrupted = jobStateService.interruptUnfinished();
        for (JobRef job : interrupted) {
            jobExecutor.handlerOf(job.type()).abandon(job);
        }
        if (!interrupted.isEmpty()) log.warn("Interrupted jobs failed at startup. count={}", interrupted.size());
    }
}
