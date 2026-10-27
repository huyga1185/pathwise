package uk.huy.pathwise.store.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;

/**
 * Invitation of a DISPATCHER or DRIVER into a store (D-16). Only the SHA-256 of the code is stored.
 */
@Entity
@Table(name = "store_invitations")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class StoreInvitation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(name = "store_id", nullable = false, updatable = false)
    private Long storeId;

    @Column(nullable = false, updatable = false)
    private String email;

    @Column(nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "code_hash", nullable = false, updatable = false, columnDefinition = "CHAR(64)")
    private String codeHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private InvitationStatus status;

    // Plain id columns: users belong to the user module (ARCHITECTURE.md §2.1).
    @Column(name = "invited_by", nullable = false, updatable = false)
    private Long invitedBy;

    @Column(name = "accepted_user_id")
    private Long acceptedUserId;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * @return {@code EXPIRED} for a pending invitation past its expiry, otherwise the stored status
     */
    public InvitationStatus effectiveStatus(Instant now) {
        return status == InvitationStatus.PENDING && !now.isBefore(expiresAt) ? InvitationStatus.EXPIRED : status;
    }
}
