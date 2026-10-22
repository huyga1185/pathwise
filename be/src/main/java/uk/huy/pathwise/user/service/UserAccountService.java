package uk.huy.pathwise.user.service;

import uk.huy.pathwise.shared.identity.UserIdentity;

import java.util.Optional;

public interface UserAccountService {
    void createUser(String email, String rawPassword);
    void updateUserPassword(long id, String newRawPassword);
    void updateUserEmail(long id, String newEmail);
    void deleteUser(long id);
    Optional<UserIdentity> findUserByCredentials(String email, String password);
    Optional<UserIdentity> findUserById(long id);
}
