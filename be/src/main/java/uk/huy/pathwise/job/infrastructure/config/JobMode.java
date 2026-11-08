package uk.huy.pathwise.job.infrastructure.config;

/**
 * Where a job runs (ARCHITECTURE.md §8.4), chosen per job type by {@code jobs.<type>.mode}.
 */
public enum JobMode {
    /** In the thread that committed the job, right after the commit. */
    INLINE,
    /** On the bounded job thread pool. */
    ASYNC
}
