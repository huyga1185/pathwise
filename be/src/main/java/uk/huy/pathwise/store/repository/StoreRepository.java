package uk.huy.pathwise.store.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.store.model.StoreStatus;

import java.util.Collection;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
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
