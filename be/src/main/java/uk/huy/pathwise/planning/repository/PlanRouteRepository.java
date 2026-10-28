package uk.huy.pathwise.planning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.planning.model.PlanRoute;
import uk.huy.pathwise.planning.model.PlanStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Routes as a driver sees them: always by store and driver, and only in plans with one of {@code statuses}.
 */
@Repository
public interface PlanRouteRepository extends JpaRepository<PlanRoute, Long> {
    @Query("SELECT r FROM PlanRoute r JOIN FETCH r.plan p WHERE r.storeId = :storeId AND r.driverId = :driverId "
            + "AND p.planDate = :planDate AND p.status IN :statuses")
    List<PlanRoute> findDriverRoutes(@Param("storeId") long storeId,
                                     @Param("driverId") long driverId,
                                     @Param("planDate") LocalDate planDate,
                                     @Param("statuses") Collection<PlanStatus> statuses);

    @Query("SELECT r FROM PlanRoute r JOIN FETCH r.plan p WHERE r.id = :id AND r.storeId = :storeId "
            + "AND r.driverId = :driverId AND p.status IN :statuses")
    Optional<PlanRoute> findDriverRoute(@Param("id") long id,
                                        @Param("storeId") long storeId,
                                        @Param("driverId") long driverId,
                                        @Param("statuses") Collection<PlanStatus> statuses);
}
