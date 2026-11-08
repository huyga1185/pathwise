package uk.huy.pathwise.user.service;

import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.user.dto.StoreUser;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Users seen as store members, for the {@code store} module. Business rules on members (who may change whom) belong
 * to the caller; this service only reads and writes users of the given store.
 */
public interface StoreUserService {
    /**
     * @return the user if it belongs to {@code storeId}; empty for another store's user (tenant isolation)
     */
    Optional<StoreUser> findInStore(long storeId, long userId);

    /**
     * @return every user of the store, ordered by id
     */
    List<StoreUser> listInStore(long storeId);

    /**
     * @return the users among {@code userIds} that belong to {@code storeId}, whatever their status; ids of other
     * stores' users are left out
     */
    List<StoreUser> findAllInStore(long storeId, Collection<Long> userIds);

    /**
     * @return true if any user, of any store, has this email (normalized, P-19)
     */
    boolean emailExists(String email);

    /**
     * Creates an ACTIVE member with a verified email, for an accepted invitation (FR-S03).
     *
     * @param phoneNumber optional; a Vietnamese number, stored as E.164
     * @throws uk.huy.pathwise.core.exception.AppException {@code EMAIL_EXISTED} if the email is taken,
     *                                                     {@code INVALID_PHONE_NUMBER} if the number is not valid
     */
    StoreUser createMember(long storeId, String email, String rawPassword, String fullName, String phoneNumber, UserRole role);

    /**
     * Changes the given fields of a member of {@code storeId}; a null argument leaves that field unchanged.
     *
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND} if the user is not in the store
     */
    StoreUser updateMember(long storeId, long userId, UserRole role, UserStatus status, Boolean canDrive);
}
