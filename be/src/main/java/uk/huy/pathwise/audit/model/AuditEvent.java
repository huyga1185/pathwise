package uk.huy.pathwise.audit.model;

import jakarta.persistence.*;
import lombok.*;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;

/**
 * One row of the audit log (T58, DATA_MODEL §2.10). Written once, never changed.
 */
@Entity
@Table(name = "audit_events")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    @Column(name = "actor_email", updatable = false)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", updatable = false)
    private UserRole actorRole;

    @Column(name = "store_id", updatable = false)
    private Long storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false)
    private AuditTargetType targetType;

    @Column(name = "target_id", updatable = false)
    private Long targetId;

    // JSON object, at most 2000 characters (cut by AuditService).
    @Column(updatable = false)
    private String details;

    @Column(name = "request_id", updatable = false)
    private String requestId;

    @Column(updatable = false)
    private String ip;
}
