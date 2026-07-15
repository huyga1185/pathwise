package uk.huy.pathwise.user.service;

import uk.huy.pathwise.user.model.User;

public interface UserAccountService {
    void createUser(String email, String rawPassword);
    void updateUserPassword(long id, String newRawPassword);
    void updateUserEmail(long id, String newEmail);
    void deleteUser(long id);
    boolean isUserValid(String email, String password);
}
