package uk.huy.pathwise.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.user.model.User;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);

    boolean existsByRole(UserRole role);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdAndStoreId(long id, long storeId);

    List<User> findAllByStoreIdOrderById(long storeId);

    List<User> findAllByStoreIdAndIdIn(long storeId, Collection<Long> ids);

    /**
     * Every user (ADMIN, T55), each filter applied only when not null.
     */
    @Query("SELECT u FROM User u WHERE (:storeId IS NULL OR u.storeId = :storeId) "
            + "AND (:role IS NULL OR u.role = :role) AND (:status IS NULL OR u.status = :status)")
    Page<User> search(@Param("storeId") Long storeId,
                      @Param("role") UserRole role,
                      @Param("status") UserStatus status,
                      Pageable pageable);

    @Query("SELECT DISTINCT u.storeId FROM User u WHERE u.role = 'OWNER' AND LOWER(u.email) LIKE :pattern ESCAPE '\\'")
    List<Long> findOwnerStoreIdsByEmailLike(@Param("pattern") String pattern);

    /**
     * @return rows {@code [storeId, email]}
     */
    @Query("SELECT u.storeId, u.email FROM User u WHERE u.role = 'OWNER' AND u.storeId IN :storeIds")
    List<Object[]> findOwnerEmails(@Param("storeIds") Collection<Long> storeIds);

    /**
     * @return rows {@code [storeId, role, count]}
     */
    @Query("SELECT u.storeId, u.role, COUNT(u) FROM User u WHERE u.storeId IN :storeIds GROUP BY u.storeId, u.role")
    List<Object[]> countByStoreAndRole(@Param("storeIds") Collection<Long> storeIds);
}
