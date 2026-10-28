package uk.huy.pathwise.planning.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.huy.pathwise.fleet.model.CapacityUnit;

import java.time.Instant;
import java.time.LocalTime;

/**
 * One assignment of a plan (driver, vehicle, shift) and, once optimized, its result. Start and end are the plan
 * depot in the MVP; they are separate columns so that routes can later start from other depots (D-20).
 */
@Entity
@Table(name = "route_plan_routes")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class PlanRoute {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false, updatable = false)
    private RoutePlan plan;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    // Plain id columns: users and vehicles belong to other modules (ARCHITECTURE.md §2.1).
    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    // Snapshot of the vehicle when the assignment is saved (D-36).
    @Column(name = "vehicle_capacity", nullable = false)
    private int vehicleCapacity;

    @Column(name = "vehicle_capacity_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private CapacityUnit vehicleCapacityUnit;

    // Local times in the plan timezone (P-14).
    @Column(name = "shift_start", nullable = false)
    private LocalTime shiftStart;

    @Column(name = "shift_end", nullable = false)
    private LocalTime shiftEnd;

    @Column(name = "max_distance_m")
    private Integer maxDistanceMeters;

    @Column(name = "start_address")
    private String startAddress;

    @Column(name = "start_lat", nullable = false)
    private double startLat;

    @Column(name = "start_lon", nullable = false)
    private double startLon;

    @Column(name = "end_address")
    private String endAddress;

    @Column(name = "end_lat", nullable = false)
    private double endLat;

    @Column(name = "end_lon", nullable = false)
    private double endLon;

    @Column(name = "planned_distance_m")
    private Integer plannedDistanceMeters;

    @Column(name = "planned_duration_s")
    private Integer plannedDurationSeconds;

    @Column(name = "planned_start")
    private Instant plannedStart;

    @Column(name = "planned_end")
    private Instant plannedEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}
