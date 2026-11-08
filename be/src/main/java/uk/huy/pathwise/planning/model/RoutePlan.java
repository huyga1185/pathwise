package uk.huy.pathwise.planning.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OptimisticLock;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A delivery plan of a store for one day (D-04): its assignments ({@link PlanRoute}) and stops ({@link PlanStop}).
 * Depot and timezone are snapshots taken when the draft is saved (D-20, P-14).
 */
@Entity
@Table(name = "route_plans")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class RoutePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    @Column(nullable = false)
    private String name;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PlanStatus status;

    @Column(name = "depot_address")
    private String depotAddress;

    @Column(name = "depot_lat", nullable = false)
    private double depotLat;

    @Column(name = "depot_lon", nullable = false)
    private double depotLon;

    // IANA zone id of the store when the draft was saved.
    @Column(nullable = false)
    private String timezone;

    @Column(name = "processing_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProcessingStatus processingStatus;

    // Plain id column: jobs belong to the job module and stay generic. Excluded from the optimistic lock: attaching the job
    // is part of the same change as setting processingStatus, which already moves the version.
    @Column(name = "active_job_id")
    @OptimisticLock(excluded = true)
    private Long activeJobId;

    // Null while there is no valid optimization result.
    @Column(name = "optimized_at")
    private Instant optimizedAt;

    @Column(name = "total_distance_m")
    private Integer totalDistanceMeters;

    @Column(name = "total_duration_s")
    private Integer totalDurationSeconds;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    // Changes to routes and stops do not increment the version on their own: the service marks the plan modified
    // once per change, so one request moves the version by exactly one.
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @BatchSize(size = 50)
    @OptimisticLock(excluded = true)
    @Builder.Default
    private List<PlanRoute> routes = new ArrayList<>();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @BatchSize(size = 50)
    @OptimisticLock(excluded = true)
    @Builder.Default
    private List<PlanStop> stops = new ArrayList<>();
}
