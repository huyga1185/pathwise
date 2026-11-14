package uk.huy.pathwise.audit.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditEvent;

import java.time.Instant;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    /**
     * Each filter applies only when not null; {@code from} inclusive, {@code to} exclusive.
     */
    @Query("SELECT e FROM AuditEvent e WHERE (:storeId IS NULL OR e.storeId = :storeId) "
            + "AND (:actorId IS NULL OR e.actorId = :actorId) AND (:action IS NULL OR e.action = :action) "
            + "AND (:from IS NULL OR e.occurredAt >= :from) AND (:to IS NULL OR e.occurredAt < :to)")
    Page<AuditEvent> search(@Param("storeId") Long storeId,
                            @Param("actorId") Long actorId,
                            @Param("action") AuditAction action,
                            @Param("from") Instant from,
                            @Param("to") Instant to,
                            Pageable pageable);

    @Modifying
    @Query("DELETE FROM AuditEvent e WHERE e.occurredAt < :cutoff")
    int deleteOccurredBefore(@Param("cutoff") Instant cutoff);
}
