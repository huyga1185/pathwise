package uk.huy.pathwise.job.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import uk.huy.pathwise.job.model.BackgroundJob;
import uk.huy.pathwise.job.model.JobStatus;

@Repository
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {
    Optional<BackgroundJob> findByIdAndStoreId(long id, long storeId);

    List<BackgroundJob> findAllByStatusIn(Collection<JobStatus> statuses);
}
