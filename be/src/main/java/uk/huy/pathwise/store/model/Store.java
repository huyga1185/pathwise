package uk.huy.pathwise.store.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Tenant (D-01). Its OWNER is the user with {@code role = OWNER} and this {@code store_id} (BR-03).
 */
@Entity
@Table(name = "stores")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "depot_address")
    private String depotAddress;

    // Both null or both set (DB check constraint).
    @Column(name = "depot_lat")
    private Double depotLat;

    @Column(name = "depot_lon")
    private Double depotLon;

    // IANA zone id; time windows and shifts are read in this zone (P-14).
    @Column(nullable = false)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StoreStatus status = StoreStatus.ACTIVE;

    // The current lock (T56); null while ACTIVE.
    @Column(name = "lock_reason")
    private String lockReason;

    @Column(name = "locked_at")
    private Instant lockedAt;

    @Column(name = "locked_by")
    private Long lockedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;
}
