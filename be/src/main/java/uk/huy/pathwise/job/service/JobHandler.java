package uk.huy.pathwise.job.service;

import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.model.JobType;
/**
 * Work of one job type, implemented by the module that owns the target (ARCHITECTURE.md §8.4). The framework records
 * the job status; the handler owns the state of its target (for example the plan's {@code processing_status}).
 * <p>
 * Handlers run outside any transaction and open their own short ones. Because an inline job runs right after the
 * commit of the request's transaction, while that transaction's resources are still bound to the thread, every
 * transaction a handler opens must use {@code Propagation.REQUIRES_NEW}.
 */
public interface JobHandler {
    JobType type();

    /**
     * Does the work and, whatever happens (use {@code finally}), releases the target. Returning normally marks the
     * job SUCCEEDED; any exception marks it FAILED.
     */
    void run(JobRef job);

    /**
     * Releases the target of a job that will never run: interrupted by a restart, or rejected by a full queue.
     */
    void abandon(JobRef job);
}
