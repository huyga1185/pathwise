package uk.huy.pathwise.planning.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.planning.model.PlanStatus;
import uk.huy.pathwise.planning.model.RoutePlan;

import java.time.Instant;
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
     * Only the date, without loading the plan: a plan loaded before {@link #findAllOfDayForUpdate} would be returned
     * as already loaded, not as read under the lock.
     */
    @Query("SELECT p.planDate FROM RoutePlan p WHERE p.id = :id AND p.storeId = :storeId")
    Optional<LocalDate> findPlanDate(@Param("id") long id, @Param("storeId") long storeId);

    /**
     * Locks every plan of the store on that day, in id order, for publishing (D-23): a shift overlap check holds only
     * if no other plan of the day can be published at the same time. Locking in one order cannot deadlock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM RoutePlan p WHERE p.storeId = :storeId AND p.planDate = :planDate ORDER BY p.id")
    List<RoutePlan> findAllOfDayForUpdate(@Param("storeId") long storeId, @Param("planDate") LocalDate planDate);

    /**
     * Moves a running plan along as its stops finish (BR-11) without touching {@code version}: a bulk update is outside
     * the optimistic lock, so the dispatcher can still cancel with the version they read (P-32). Pending changes are
     * flushed first; the plan already loaded keeps its old status.
     *
     * @param completedAt set with COMPLETED, null otherwise
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE RoutePlan p SET p.status = :status, p.completedAt = :completedAt WHERE p.id = :id")
    void updateStatus(@Param("id") long id, @Param("status") PlanStatus status, @Param("completedAt") Instant completedAt);

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

    // --- Across stores, for the ADMIN area (T55). Never for a store member's request.

    /**
     * @return {@code [storeId, plan count, latest updatedAt]} rows, only for stores that have plans
     */
    @Query("SELECT p.storeId, COUNT(p), MAX(p.updatedAt) FROM RoutePlan p WHERE p.storeId IN :storeIds GROUP BY p.storeId")
    List<Object[]> summarizeByStore(@Param("storeIds") Collection<Long> storeIds);

    /**
     * @return {@code [storeId, latest statusUpdatedAt]} rows, only for stores with a reported stop
     */
    @Query("SELECT s.storeId, MAX(s.statusUpdatedAt) FROM PlanStop s "
            + "WHERE s.storeId IN :storeIds AND s.statusUpdatedAt IS NOT NULL GROUP BY s.storeId")
    List<Object[]> lastStopResultByStore(@Param("storeIds") Collection<Long> storeIds);

    @Query("SELECT COUNT(p) FROM RoutePlan p WHERE p.storeId = :storeId AND p.createdAt >= :since")
    long countCreatedSince(@Param("storeId") long storeId, @Param("since") Instant since);

    @Query("SELECT COUNT(p) FROM RoutePlan p WHERE p.storeId = :storeId AND p.publishedAt >= :since")
    long countPublishedSince(@Param("storeId") long storeId, @Param("since") Instant since);

    @Query("SELECT COUNT(p) FROM RoutePlan p WHERE p.storeId = :storeId AND p.completedAt >= :since")
    long countCompletedSince(@Param("storeId") long storeId, @Param("since") Instant since);

    /**
     * @return {@code [status, count]} rows of the stop results reported since {@code since}
     */
    @Query("SELECT s.status, COUNT(s) FROM PlanStop s WHERE s.storeId = :storeId AND s.statusUpdatedAt >= :since "
            + "GROUP BY s.status")
    List<Object[]> countStopResultsSince(@Param("storeId") long storeId, @Param("since") Instant since);

    /**
     * @return {@code [day, count]} of plans by created_at in {@code [from, to)}, by day at {@code offset} seconds (T60)
     */
    @Query(value = "SELECT DATE(DATE_ADD(created_at, INTERVAL :offset SECOND)) AS d, COUNT(*) FROM route_plans "
            + "WHERE created_at >= :from AND created_at < :to GROUP BY d", nativeQuery = true)
    List<Object[]> countCreatedByDay(@Param("from") Instant from, @Param("to") Instant to, @Param("offset") int offset);

    /**
     * @return {@code [day, count]} of plans by published_at in {@code [from, to)}, by day at {@code offset} seconds (T60)
     */
    @Query(value = "SELECT DATE(DATE_ADD(published_at, INTERVAL :offset SECOND)) AS d, COUNT(*) FROM route_plans "
            + "WHERE published_at >= :from AND published_at < :to GROUP BY d", nativeQuery = true)
    List<Object[]> countPublishedByDay(@Param("from") Instant from, @Param("to") Instant to, @Param("offset") int offset);

    /**
     * @return {@code [day, count]} of plans by completed_at in {@code [from, to)}, by day at {@code offset} seconds (T60)
     */
    @Query(value = "SELECT DATE(DATE_ADD(completed_at, INTERVAL :offset SECOND)) AS d, COUNT(*) FROM route_plans "
            + "WHERE completed_at >= :from AND completed_at < :to GROUP BY d", nativeQuery = true)
    List<Object[]> countCompletedByDay(@Param("from") Instant from, @Param("to") Instant to, @Param("offset") int offset);

    /**
     * @return {@code [day, status, count]} of stop results reported in {@code [from, to)} (T60)
     */
    @Query(value = "SELECT DATE(DATE_ADD(status_updated_at, INTERVAL :offset SECOND)) AS d, status, COUNT(*) "
            + "FROM route_plan_stops WHERE status_updated_at >= :from AND status_updated_at < :to AND status <> 'PENDING' "
            + "GROUP BY d, status", nativeQuery = true)
    List<Object[]> countStopResultsByDay(@Param("from") Instant from, @Param("to") Instant to, @Param("offset") int offset);

    /**
     * @return stores that published a plan since {@code since} (T60: "active")
     */
    @Query("SELECT COUNT(DISTINCT p.storeId) FROM RoutePlan p WHERE p.publishedAt >= :since")
    long countStoresPublishingSince(@Param("since") Instant since);
}
