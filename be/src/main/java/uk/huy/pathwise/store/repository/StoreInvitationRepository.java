package uk.huy.pathwise.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.store.model.InvitationStatus;
import uk.huy.pathwise.store.model.StoreInvitation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface StoreInvitationRepository extends JpaRepository<StoreInvitation, Long> {
    Optional<StoreInvitation> findByIdAndStoreId(long id, long storeId);

    Optional<StoreInvitation> findByCodeHash(String codeHash);

    List<StoreInvitation> findAllByStoreIdOrderByIdDesc(long storeId);

    List<StoreInvitation> findAllByStoreIdAndEmailAndStatus(long storeId, String email, InvitationStatus status);

    /**
     * Invitations nobody can use any more since before {@code cutoff} (T53): expired (pending past their expiry, or
     * stored as expired) or revoked. Accepted ones stay: they say who invited whom.
     */
    @Modifying
    @Query("DELETE FROM StoreInvitation i WHERE i.status <> 'ACCEPTED' AND (i.expiresAt < :cutoff "
            + "OR (i.status = 'REVOKED' AND i.updatedAt < :cutoff))")
    int deleteUnusableBefore(@Param("cutoff") Instant cutoff);
}
