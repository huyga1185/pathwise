package uk.huy.pathwise.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.store.model.InvitationStatus;
import uk.huy.pathwise.store.model.StoreInvitation;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreInvitationRepository extends JpaRepository<StoreInvitation, Long> {
    Optional<StoreInvitation> findByIdAndStoreId(long id, long storeId);

    Optional<StoreInvitation> findByCodeHash(String codeHash);

    List<StoreInvitation> findAllByStoreIdOrderByIdDesc(long storeId);

    List<StoreInvitation> findAllByStoreIdAndEmailAndStatus(long storeId, String email, InvitationStatus status);
}
