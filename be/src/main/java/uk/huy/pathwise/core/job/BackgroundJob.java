package uk.huy.pathwise.core.job;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * A job shared by every module (D-19, ARCHITECTURE.md §8.4). Only the record exists so far: the executor and the
 * handlers come with T29.
 */
@Entity
@Table(name = "background_jobs")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class BackgroundJob {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private JobType type;

    // The plan id for every current type.
    @Column(name = "target_id", nullable = false, updatable = false)
    private Long targetId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}
