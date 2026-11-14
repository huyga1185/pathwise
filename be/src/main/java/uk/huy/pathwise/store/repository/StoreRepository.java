package uk.huy.pathwise.store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.shared.store.StoreStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    /**
     * @return {@code [day, count]} of stores created in {@code [from, to)}, by day at {@code offset} seconds from UTC
     */
    @Query(value = "SELECT DATE(DATE_ADD(created_at, INTERVAL :offset SECOND)) AS d, COUNT(*) FROM stores "
            + "WHERE created_at >= :from AND created_at < :to GROUP BY d", nativeQuery = true)
    List<Object[]> countCreatedByDay(@Param("from") Instant from, @Param("to") Instant to, @Param("offset") int offset);

    long countByStatus(StoreStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Store s WHERE s.id = :id")
    Optional<Store> findByIdForUpdate(@Param("id") long id);

    @Query("SELECT s.status FROM Store s WHERE s.id = :id")
    Optional<StoreStatus> findStatusById(@Param("id") long id);

    /**
     * Every store (ADMIN, T55): by state, and when {@code filtered} by name pattern or id.
     */
    @Query("SELECT s FROM Store s WHERE (:status IS NULL OR s.status = :status) "
            + "AND (:filtered = false OR LOWER(s.name) LIKE :pattern ESCAPE '\\' OR s.id IN :ids)")
    Page<Store> search(@Param("filtered") boolean filtered,
                       @Param("pattern") String pattern,
                       @Param("ids") Collection<Long> ids,
                       @Param("status") StoreStatus status,
                       Pageable pageable);
}
