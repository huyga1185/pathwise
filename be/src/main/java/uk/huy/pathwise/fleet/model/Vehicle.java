package uk.huy.pathwise.fleet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * A vehicle of a store (FR-V01). Deactivated rather than deleted, so plans that used it keep their reference.
 */
@Entity
@Table(name = "vehicles")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    // Plain id column: stores belong to the store module (ARCHITECTURE.md §2.1).
    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    // Unique per store; the comparison follows the column collation (case-insensitive by default).
    @Column(nullable = false)
    private String name;

    @Column(name = "plate_number")
    private String plateNumber;

    // Whole number above 0 (P-14), counted in capacityUnit (D-36).
    @Column(nullable = false)
    private int capacity;

    @Column(name = "capacity_unit", nullable = false)
    @Enumerated(EnumType.STRING)
    private CapacityUnit capacityUnit;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}
