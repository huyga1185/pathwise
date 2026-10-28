package uk.huy.pathwise.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.model.BackgroundJob;
import uk.huy.pathwise.job.model.JobErrors;
import uk.huy.pathwise.job.model.JobStatus;
import uk.huy.pathwise.job.repository.BackgroundJobRepository;

/**
 * Status changes of jobs, each in its own transaction ({@code REQUIRES_NEW}: an inline job runs while the committed
 * request transaction is still bound to the thread, see {@link JobHandler}).
 */
@Service
@RequiredArgsConstructor
class JobStateService {
    private final BackgroundJobRepository jobRepository;

    /**
     * @return false if the job is no longer QUEUED (for example interrupted at startup), so it must not run
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markRunning(long jobId) {
        BackgroundJob job = jobRepository.findById(jobId).orElseThrow();
        if (job.getStatus() != JobStatus.QUEUED) return false;
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job.setAttempts(job.getAttempts() + 1);
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSucceeded(long jobId) {
        finish(jobId, JobStatus.SUCCEEDED, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(long jobId, String errorCode) {
        finish(jobId, JobStatus.FAILED, errorCode);
    }

    /**
     * Fails every QUEUED or RUNNING job as {@link JobErrors#INTERRUPTED}: in a single instance (ARCHITECTURE.md §8.4)
     * none of them can still be running or queued after a restart, since the queue lives in memory.
     *
     * @return the interrupted jobs, for their handlers to release the targets
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<JobRef> interruptUnfinished() {
        List<BackgroundJob> jobs = jobRepository.findAllByStatusIn(List.of(JobStatus.QUEUED, JobStatus.RUNNING));
        Instant now = Instant.now();
        for (BackgroundJob job : jobs) {
            job.setStatus(JobStatus.FAILED);
            job.setErrorCode(JobErrors.INTERRUPTED);
            job.setFinishedAt(now);
        }
        return jobs.stream().map(JobStateService::toRef).toList();
    }

    static JobRef toRef(BackgroundJob job) {
        return new JobRef(job.getId(), job.getType(), job.getStoreId(), job.getTargetId());
    }

    private void finish(long jobId, JobStatus status, String errorCode) {
        BackgroundJob job = jobRepository.findById(jobId).orElseThrow();
        job.setStatus(status);
        job.setErrorCode(errorCode);
        job.setFinishedAt(Instant.now());
    }
}
