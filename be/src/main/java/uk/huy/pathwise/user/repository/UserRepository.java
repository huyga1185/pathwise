package uk.huy.pathwise.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.user.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndStoreId(long id, long storeId);

    List<User> findAllByStoreIdOrderById(long storeId);

    List<User> findAllByStoreIdAndIdIn(long storeId, Collection<Long> ids);
}
