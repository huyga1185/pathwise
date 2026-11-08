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

    List<RefreshToken> findByFamilyIdAndState(String familyId, RefreshTokenState state);

    @Modifying
    @Query("UPDATE RefreshToken rf SET rf.state = 'REVOKED' WHERE rf.familyId = :familyId")
    void revokeFamily(@Param("familyId") String familyId);

    List<RefreshToken> findByUserIdAndStateAndExpiresAtAfter(Long userId, RefreshTokenState state, Instant now);

    boolean existsByUserIdAndFamilyIdAndStateAndExpiresAtAfter(Long userId, String familyId, RefreshTokenState state, Instant now);

    /**
     * Revokes every family of the user except {@code exceptFamilyId} (null: all families).
     */
    @Modifying
    @Query("UPDATE RefreshToken rf SET rf.state = 'REVOKED' WHERE rf.userId = :userId AND rf.state <> 'REVOKED' "
            + "AND (:exceptFamilyId IS NULL OR rf.familyId <> :exceptFamilyId)")
    void revokeAllForUser(@Param("userId") Long userId, @Param("exceptFamilyId") String exceptFamilyId);
}
