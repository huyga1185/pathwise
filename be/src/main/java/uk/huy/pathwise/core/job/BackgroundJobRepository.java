package uk.huy.pathwise.core.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {
    Optional<BackgroundJob> findByIdAndStoreId(long id, long storeId);
}
