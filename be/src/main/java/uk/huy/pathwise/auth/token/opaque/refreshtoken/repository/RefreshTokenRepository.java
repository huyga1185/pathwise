package uk.huy.pathwise.auth.token.opaque.refreshtoken.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshToken;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshTokenState;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rf FROM RefreshToken rf WHERE rf.token = :token")
    Optional<RefreshToken> findByTokenForUpdate(@Param("token") String token);

    /**
     * Locking read: a plain read here would open a read view, and MariaDB 11.6+ then fails the later update of a token
     * another rotation changed meanwhile (error 1020, F-44). Waits for that rotation and returns the latest state.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rf FROM RefreshToken rf WHERE rf.familyId = :familyId AND rf.state = :state")
    List<RefreshToken> findByFamilyIdAndStateForUpdate(@Param("familyId") String familyId,
                                                       @Param("state") RefreshTokenState state);

    @Modifying
    @Query("UPDATE RefreshToken rf SET rf.state = 'REVOKED' WHERE rf.familyId = :familyId")
    void revokeFamily(@Param("familyId") String familyId);

    List<RefreshToken> findByUserIdAndStateAndExpiresAtAfter(Long userId, RefreshTokenState state, Instant now);

    /**
     * Tokens of one family owned by {@code userId}, locked: same reason as {@link #findByFamilyIdAndStateForUpdate}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT rf FROM RefreshToken rf WHERE rf.userId = :userId AND rf.familyId = :familyId")
    List<RefreshToken> findByUserIdAndFamilyIdForUpdate(@Param("userId") Long userId, @Param("familyId") String familyId);

    /**
     * Revokes every family of the user except {@code exceptFamilyId} (null: all families).
     */
    @Modifying
    @Query("UPDATE RefreshToken rf SET rf.state = 'REVOKED' WHERE rf.userId = :userId AND rf.state <> 'REVOKED' "
            + "AND (:exceptFamilyId IS NULL OR rf.familyId <> :exceptFamilyId)")
    void revokeAllForUser(@Param("userId") Long userId, @Param("exceptFamilyId") String exceptFamilyId);

    /**
     * Tokens expired before {@code cutoff}, and tokens revoked before it (T53). A used token of a live session expires
     * within the refresh lifetime, well before any cutoff, so reuse detection keeps working.
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rf WHERE rf.expiresAt < :cutoff OR (rf.state = 'REVOKED' AND rf.updatedAt < :cutoff)")
    int deleteStale(@Param("cutoff") Instant cutoff);
}
