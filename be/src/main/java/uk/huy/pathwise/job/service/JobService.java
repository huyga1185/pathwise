package uk.huy.pathwise.job.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.dto.JobResponse;
import uk.huy.pathwise.job.model.BackgroundJob;
import uk.huy.pathwise.job.model.JobStatus;
import uk.huy.pathwise.job.model.JobType;
import uk.huy.pathwise.job.repository.BackgroundJobRepository;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.util.Optional;

/**
 * Public API of the job framework (D-19, ARCHITECTURE.md §8.4): create jobs and read them.
 */
@Service
@RequiredArgsConstructor
public class JobService {
    private final BackgroundJobRepository jobRepository;
    private final JobExecutor jobExecutor;

    /**
     * Records a QUEUED job in the caller's transaction and submits it once that transaction commits, so the job never
     * sees uncommitted data and is never submitted for a rolled back change.
     *
     * @return the new job id
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public long createJob(long storeId, JobType type, long targetId, long createdBy) {
        BackgroundJob job = jobRepository.save(BackgroundJob.builder()
                .storeId(storeId)
                .type(type)
                .targetId(targetId)
                .status(JobStatus.QUEUED)
                .createdBy(createdBy)
                .build());
        JobRef ref = JobStateService.toRef(job);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                jobExecutor.submit(ref);
            }
        });
        return job.getId();
    }

    /**
     * @return the job if it belongs to {@code storeId}
     */
    @Transactional(readOnly = true)
    public Optional<JobResponse> findJob(long storeId, long jobId) {
        return jobRepository.findByIdAndStoreId(jobId, storeId).map(JobService::toResponse);
    }

    /**
     * @throws AppException {@code NOT_FOUND} if the job is not in the caller's store (BR-01)
     */
    @Transactional(readOnly = true)
    public JobResponse getJob(UserIdentity actor, long jobId) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        return findJob(actor.storeId(), jobId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private static JobResponse toResponse(BackgroundJob job) {
        return new JobResponse(job.getId(), job.getType(), job.getStatus(), job.getTargetId(), job.getErrorCode(),
                job.getCreatedAt(), job.getStartedAt(), job.getFinishedAt());
    }
}
