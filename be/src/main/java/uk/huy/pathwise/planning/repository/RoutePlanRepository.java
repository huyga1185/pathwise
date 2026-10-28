package uk.huy.pathwise.planning.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.RoutePlan;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoutePlanRepository extends JpaRepository<RoutePlan, Long> {
    Optional<RoutePlan> findByIdAndStoreId(long id, long storeId);

    /**
     * For every change to a plan: the row lock makes the {@code version} check and the change one step, so two
     * concurrent edits cannot both pass the check.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM RoutePlan p WHERE p.id = :id AND p.storeId = :storeId")
    Optional<RoutePlan> findByIdAndStoreIdForUpdate(@Param("id") long id, @Param("storeId") long storeId);

    /**
     * @param planDate only this day, if set
     * @param status   only this status, if set
     */
    @Query("SELECT p FROM RoutePlan p WHERE p.storeId = :storeId "
            + "AND (:planDate IS NULL OR p.planDate = :planDate) AND (:status IS NULL OR p.status = :status)")
    Page<RoutePlan> findPage(@Param("storeId") long storeId,
                             @Param("planDate") LocalDate planDate,
                             @Param("status") PlanStatus status,
                             Pageable pageable);

    /**
     * @return {@code [planId, count]} rows, only for plans that have stops
     */
    @Query("SELECT s.plan.id, COUNT(s) FROM PlanStop s WHERE s.plan.id IN :planIds GROUP BY s.plan.id")
    List<Object[]> countStops(@Param("planIds") Collection<Long> planIds);

    /**
     * @return {@code [planId, count]} rows, only for plans that have routes
     */
    @Query("SELECT r.plan.id, COUNT(r) FROM PlanRoute r WHERE r.plan.id IN :planIds GROUP BY r.plan.id")
    List<Object[]> countRoutes(@Param("planIds") Collection<Long> planIds);
}
