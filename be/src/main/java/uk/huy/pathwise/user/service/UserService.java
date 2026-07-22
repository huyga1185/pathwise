package uk.huy.pathwise.user.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;
import uk.huy.pathwise.shared.pagination.PageResponse;
import uk.huy.pathwise.shared.pagination.PaginationValidator;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.user.mapper.UserMapper;
import uk.huy.pathwise.user.model.User;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.repository.UserRepository;

import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserService implements UserAccountService, UserProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private static final Set<String> USER_SORT_WHITELIST = Set.of("id", "email", "createdAt", "updatedAt");

    private User getUserById (long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COULD_NOT_FIND_USER));
    }

    private void checkUserEmailExisted(String email) {
        if (userRepository.existsByEmail(email))
            throw new AppException(ErrorCode.EMAIL_EXISTED);
    }

    @Override
    @Transactional
    public void createUser(String email, String rawPassword) {
        checkUserEmailExisted(email);
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = User.builder()
                .email(email)
                .password(hashedPassword).build();
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUser(UserIdentity identity, UserModificationRequest request) {
        if (identity == null) throw new NullPointerException("Identity is null");
        User user = getUserById(identity.id());
        userMapper.update(user, request);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserPassword(long id, String newRawPassword) {
        User user = getUserById(id);
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserEmail(long id, String newEmail) {
        checkUserEmailExisted(newEmail);
        User user = getUserById(id);
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    @Override
    public Optional<UserIdentity> findUserByCredentials(String email, String password) {
        if (email == null) throw new NullPointerException("Email is null");
        if (password == null) throw new NullPointerException("Password is null");
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) return Optional.empty();
        if (!passwordEncoder.matches(password, user.get().getPassword())) return Optional.empty();
        return Optional.of(userMapper.toUserIdentity(user.get()));
    }

    @Override
    public GetUserResponse getUser(UserIdentity identity) {
        if (identity == null) throw new NullPointerException("Identity is null");
        User user = getUserById(identity.id());
        return userMapper.toGetUserResponse(user);
    }

    @Override
    public PageResponse<AdminGetUserResponse> getListUser(Pageable pageable) {
        PaginationValidator.validateSort(pageable, USER_SORT_WHITELIST);
        Page<User> users = userRepository.findAll(pageable);
        return PageResponse.from(users.map(userMapper::toAdminGetUserResponse));
    }
}
