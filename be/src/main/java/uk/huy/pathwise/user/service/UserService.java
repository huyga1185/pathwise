package uk.huy.pathwise.user.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.core.pagination.PaginationValidator;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.user.dto.AuthenticationResult;
import uk.huy.pathwise.user.dto.AccountState;
import uk.huy.pathwise.user.dto.StoreUser;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.user.infrastructure.mapper.UserMapper;
import uk.huy.pathwise.user.model.User;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.repository.UserRepository;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserService implements UserAccountService, UserProfileService, StoreUserQuery {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private static final Set<String> USER_SORT_WHITELIST = Set.of("id", "email", "createdAt", "updatedAt");

    private User getUserById (long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void checkUserEmailExisted(String email) {
        if (userRepository.existsByEmail(email))
            throw new AppException(ErrorCode.EMAIL_EXISTED);
    }

    /**
     * Emails are unique whatever their case (BR-02, P-19).
     */
    private static String normalizeEmail(String email) {
        if (email == null) throw new NullPointerException("Email is null");
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    @Transactional
    public UserIdentity createOwner(String email, String rawPassword, String fullName, long storeId) {
        String normalizedEmail = normalizeEmail(email);
        // Two concurrent registrations can both pass this check; the unique key then answers 409 CONFLICT (F-15).
        checkUserEmailExisted(normalizedEmail);
        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(UserRole.OWNER)
                .status(UserStatus.PENDING_VERIFICATION)
                .storeId(storeId)
                .build();
        return userMapper.toUserIdentity(userRepository.save(user));
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
        String normalizedEmail = normalizeEmail(newEmail);
        checkUserEmailExisted(normalizedEmail);
        User user = getUserById(id);
        user.setEmail(normalizedEmail);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    @Override
    public AuthenticationResult authenticate(String email, String password) {
        if (password == null) throw new NullPointerException("Password is null");
        return userRepository.findByEmail(normalizeEmail(email))
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(this::toAuthenticationResult)
                .orElseGet(AuthenticationResult.InvalidCredentials::new);
    }

    private AuthenticationResult toAuthenticationResult(User user) {
        return switch (user.getStatus()) {
            case ACTIVE -> new AuthenticationResult.Authenticated(userMapper.toUserIdentity(user), user.getFullName());
            case PENDING_VERIFICATION -> new AuthenticationResult.EmailNotVerified();
            case DISABLED -> new AuthenticationResult.Disabled();
        };
    }

    @Override
    public Optional<UserIdentity> findUserById(long id) {
        return userRepository.findById(id).map(userMapper::toUserIdentity);
    }

    @Override
    public Optional<UserIdentity> findActiveUserById(long id) {
        return userRepository.findById(id)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .map(userMapper::toUserIdentity);
    }

    @Override
    public Optional<AccountState> findAccountState(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(user -> new AccountState(user.getId(),
                        user.getEmail(),
                        user.getStatus(),
                        user.getEmailVerifiedAt() != null));
    }

    @Override
    @Transactional
    public void markEmailVerified(long id) {
        User user = getUserById(id);
        user.setEmailVerifiedAt(Instant.now());
        // A disabled account stays disabled; verifying the email does not re-enable it.
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Override
    public Optional<StoreUser> findInStore(long storeId, long userId) {
        return userRepository.findByIdAndStoreId(userId, storeId).map(userMapper::toStoreUser);
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
