package uk.huy.pathwise.user.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private Instant updatedAt;

    /**
      * Create a user only needs email and password, but role is required (non-null).
      * Centralizing the default here means every creation path gets a role
      * automatically, so no caller can forget to set it and fail to persist.
      */
    @PrePersist
    private void applyDefaultRole() {
        if (role != null) return;
        this.role = UserRole.USER;
    }
}
