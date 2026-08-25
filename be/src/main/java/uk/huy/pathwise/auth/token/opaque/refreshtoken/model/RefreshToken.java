package uk.huy.pathwise.auth.token.opaque.refreshtoken.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Long userId;

    @JoinColumn(name = "family_id", nullable = false)
    private String familyId;

    @JoinColumn(name = "user_agent")
    private String userAgent;

    @JoinColumn(name = "ip_address")
    private String ipAddress;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RefreshTokenState state;

    @Column(name = "rotated_at")
    private Instant rotatedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * Establishes the initial lifecycle state for a refresh token before persistence.
     */
    @PrePersist
    private void applyDefaultState() {
        if (state != null) return;
        this.state = RefreshTokenState.ACTIVE;
    }
}
