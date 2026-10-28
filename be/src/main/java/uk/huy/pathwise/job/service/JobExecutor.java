package uk.huy.pathwise.job.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.infrastructure.config.JobMode;
import uk.huy.pathwise.job.infrastructure.config.JobProperties;
import uk.huy.pathwise.job.model.JobErrors;
import uk.huy.pathwise.job.model.JobType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Runs submitted jobs inline or on a bounded thread pool, by {@code jobs.<type>.mode} (ARCHITECTURE.md §8.4). The
 * pool is private to this class rather than a Spring {@code Executor} bean, so that it does not replace Spring Boot's
 * default task executor.
 */
@Slf4j
@Component
public class JobExecutor {
    private final JobProperties properties;
    private final JobStateService jobStateService;
    private final Map<JobType, JobHandler> handlers = new EnumMap<>(JobType.class);
    private final ThreadPoolTaskExecutor pool;

    JobExecutor(JobProperties properties, JobStateService jobStateService, List<JobHandler> handlers) {
        this.properties = properties;
        this.jobStateService = jobStateService;
        for (JobHandler handler : handlers) {
            if (this.handlers.put(handler.type(), handler) != null)
                throw new IllegalStateException("Two handlers for job type " + handler.type());
        }
        pool = new ThreadPoolTaskExecutor();
        pool.setCorePoolSize(properties.executor().poolSize());
        pool.setMaxPoolSize(properties.executor().poolSize());
        pool.setQueueCapacity(properties.executor().queueCapacity());
        pool.setThreadNamePrefix("job-");
        pool.initialize();
    }

    /**
     * Runs the job now (inline) or queues it (async). Called only after the transaction that created the job has
     * committed. A job rejected by a full queue fails as {@link JobErrors#JOB_QUEUE_FULL} and its target is released.
     */
    public void submit(JobRef job) {
        JobHandler handler = handlerOf(job.type());
        if (properties.modeOf(job.type()) == JobMode.INLINE) {
            run(handler, job);
            return;
        }
        try {
            pool.execute(() -> run(handler, job));
        } catch (TaskRejectedException e) {
            log.warn("Job queue full, job rejected. jobId={}, type={}", job.id(), job.type());
            jobStateService.markFailed(job.id(), JobErrors.JOB_QUEUE_FULL);
            handler.abandon(job);
        }
    }

    JobHandler handlerOf(JobType type) {
        JobHandler handler = handlers.get(type);
        if (handler == null) throw new IllegalStateException("No handler for job type " + type);
        return handler;
    }

    private void run(JobHandler handler, JobRef job) {
        if (!jobStateService.markRunning(job.id())) return;
        try {
            handler.run(job);
            jobStateService.markSucceeded(job.id());
        } catch (Exception e) {
            String errorCode = errorCodeOf(e);
            log.error("Job failed. jobId={}, type={}, errorCode={}", job.id(), job.type(), errorCode, e);
            jobStateService.markFailed(job.id(), errorCode);
        }
    }

    private static String errorCodeOf(Exception e) {
        if (e instanceof ExternalServiceException external) return external.getErrorCode().name();
        if (e instanceof AppException app) return app.getErrorCode().name();
        return JobErrors.INTERNAL_ERROR;
    }

    @PreDestroy
    void shutdown() {
        // Jobs still queued or running are failed as INTERRUPTED at the next startup.
        pool.shutdown();
    }
}
