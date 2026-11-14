package uk.huy.pathwise.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import uk.huy.pathwise.job.model.BackgroundJob;
import uk.huy.pathwise.job.model.JobStatus;

@Repository
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {
    Optional<BackgroundJob> findByIdAndStoreId(long id, long storeId);

    List<BackgroundJob> findAllByStatusIn(Collection<JobStatus> statuses);

    /**
     * Jobs finished before {@code cutoff} (T53). A job still named as a plan's active job is kept: the foreign key from
     * {@code route_plans.active_job_id} would refuse the whole delete.
     */
    /**
     * @return {@code [day, count, failed]} of optimizations created in {@code [from, to)} (T60)
     */
    @Query(value = "SELECT DATE(DATE_ADD(created_at, INTERVAL :offset SECOND)) AS d, COUNT(*), "
            + "SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END) FROM background_jobs "
            + "WHERE type = 'OPTIMIZE_PLAN' AND created_at >= :from AND created_at < :to GROUP BY d", nativeQuery = true)
    List<Object[]> countOptimizationsByDay(@Param("from") Instant from, @Param("to") Instant to, @Param("offset") int offset);

    long countByStatusIn(Collection<JobStatus> statuses);

    long countByStatusAndFinishedAtGreaterThanEqual(JobStatus status, Instant since);

    @Modifying
    @Query(value = "DELETE FROM background_jobs WHERE status IN ('SUCCEEDED', 'FAILED') AND finished_at < :cutoff "
            + "AND NOT EXISTS (SELECT 1 FROM route_plans p WHERE p.active_job_id = background_jobs.id)", nativeQuery = true)
    int deleteFinishedBefore(@Param("cutoff") Instant cutoff);
}
