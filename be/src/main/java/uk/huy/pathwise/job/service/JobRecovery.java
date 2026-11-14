package uk.huy.pathwise.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.core.observability.FailureSource;
import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.model.JobErrors;

import java.util.List;

/**
 * At startup, fails the jobs left QUEUED or RUNNING by the previous run and lets their handlers release the targets
 * (plan back to IDLE). Valid only with a single instance (ARCHITECTURE.md §8.4).
 */
@Component
@RequiredArgsConstructor
public class JobRecovery {
    private final JobStateService jobStateService;
    private final JobExecutor jobExecutor;
    private final FailureLog failureLog;

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedJobs() {
        List<JobRef> interrupted = jobStateService.interruptUnfinished();
        for (JobRef job : interrupted) {
            jobExecutor.handlerOf(job.type()).abandon(job);
            failureLog.record(FailureSource.JOB, job.type().name(), JobErrors.INTERRUPTED, "jobId=" + job.id(), null);
        }
    }
}
