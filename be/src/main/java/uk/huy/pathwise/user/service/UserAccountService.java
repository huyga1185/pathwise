package uk.huy.pathwise.user.service;

import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.dto.AuthenticationResult;
import uk.huy.pathwise.user.dto.AccountState;

import java.util.Optional;

/**
 * Account operations for the {@code auth} module. Every email argument is normalized (trim, lower case, P-19).
 */
public interface UserAccountService {
    /**
     * Creates the OWNER of {@code storeId}, waiting for email verification.
     *
     * @return the new user, with its normalized email
     * @throws uk.huy.pathwise.core.exception.AppException {@code EMAIL_EXISTED} if the email is taken
     */
    UserIdentity createOwner(String email, String rawPassword, String fullName, long storeId);

    void updateUserPassword(long id, String newRawPassword);

    /**
     * @return true if {@code rawPassword} is the current password of user {@code id}
     * @throws uk.huy.pathwise.core.exception.AppException {@code NOT_FOUND} if there is no such user
     */
    boolean matchesPassword(long id, String rawPassword);
    void updateUserEmail(long id, String newEmail);
    void deleteUser(long id);
    AuthenticationResult authenticate(String email, String password);
    Optional<UserIdentity> findUserById(long id);

    /**
     * @return the user only if it exists and is {@code ACTIVE} (BR-04)
     */
    Optional<UserIdentity> findActiveUserById(long id);

    Optional<AccountState> findAccountState(String email);

    /**
     * Records the email as verified now; a {@code PENDING_VERIFICATION} account becomes {@code ACTIVE}.
     */
    void markEmailVerified(long id);
}
