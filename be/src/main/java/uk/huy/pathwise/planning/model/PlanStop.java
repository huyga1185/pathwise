package uk.huy.pathwise.planning.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.huy.pathwise.fleet.model.CapacityUnit;

import java.time.Instant;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;

/**
 * A place to deliver to in a plan. It has an address, coordinates, or both (BR-08); {@link #geocodeStatus} says where
 * the coordinates come from (D-18).
 */
@Entity
@Table(name = "route_plan_stops")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PlanStop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private RoutePlan plan;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    // Null while not assigned: plan not optimized, or dropped by the solver (P-13).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    private PlanRoute route;

    // 1..n within the route.
    private Integer sequence;

    @Column(nullable = false)
    private String label;

    @Column(name = "recipient_name")
    private String recipientName;

    // E.164.
    @Column(name = "recipient_phone")
    private String recipientPhone;

    @Column(name = "address_text")
    private String addressText;

    private Double lat;

    private Double lon;

    @Column(name = "geocode_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private GeocodeStatus geocodeStatus;

    @Column(name = "geocode_error")
    @Enumerated(EnumType.STRING)
    private GeocodeError geocodeError;

    @Column(name = "geocoded_display_name")
    private String geocodedDisplayName;

    // Per capacity unit (D-36); a missing unit is 0 and ORDER is never stored (every stop counts 1).
    @ElementCollection
    @CollectionTable(name = "route_plan_stop_demands", joinColumns = @JoinColumn(name = "stop_id"))
    @MapKeyColumn(name = "unit")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "quantity", nullable = false)
    @BatchSize(size = 100)
    @Builder.Default
    private Map<CapacityUnit, Integer> demands = new EnumMap<>(CapacityUnit.class);

    @Column(name = "service_time_s", nullable = false)
    private int serviceTimeSeconds;

    // Both null or both set, start before end; local times in the plan timezone (P-14).
    @Column(name = "tw_start")
    private LocalTime timeWindowStart;

    @Column(name = "tw_end")
    private LocalTime timeWindowEnd;

    private String note;

    @Column(name = "planned_arrival")
    private Instant plannedArrival;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StopStatus status;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "status_updated_at")
    private Instant statusUpdatedAt;

    @Column(name = "status_updated_by")
    private Long statusUpdatedBy;

    @Version
    @Column(nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}
