package uk.huy.pathwise.user.service;

import com.google.i18n.phonenumbers.NumberParseException;
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
import uk.huy.pathwise.user.infrastructure.phonenumber.PhoneCheck;
import uk.huy.pathwise.user.infrastructure.phonenumber.PhoneNumberProcessor;
import uk.huy.pathwise.user.model.User;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@AllArgsConstructor
public class UserService implements UserAccountService, UserProfileService, StoreUserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final PhoneNumberProcessor phoneNumberProcessor;
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
    public boolean matchesPassword(long id, String rawPassword) {
        if (rawPassword == null) throw new NullPointerException("Password is null");
        return passwordEncoder.matches(rawPassword, getUserById(id).getPassword());
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
    public List<StoreUser> listInStore(long storeId) {
        return userRepository.findAllByStoreIdOrderById(storeId).stream().map(userMapper::toStoreUser).toList();
    }

    @Override
    public boolean emailExists(String email) {
        return userRepository.existsByEmail(normalizeEmail(email));
    }

    @Override
    @Transactional
    public StoreUser createMember(long storeId, String email, String rawPassword, String fullName, String phoneNumber,
                                  UserRole role) {
        String normalizedEmail = normalizeEmail(email);
        checkUserEmailExisted(normalizedEmail);
        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .phoneNumber(toE164(phoneNumber))
                .role(role)
                .status(UserStatus.ACTIVE)
                .storeId(storeId)
                .emailVerifiedAt(Instant.now())
                .build();
        return userMapper.toStoreUser(userRepository.save(user));
    }

    @Override
    @Transactional
    public StoreUser updateMember(long storeId, long userId, UserRole role, UserStatus status, Boolean canDrive) {
        User user = userRepository.findByIdAndStoreId(userId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (role != null) user.setRole(role);
        if (status != null) user.setStatus(status);
        if (canDrive != null) user.setCanDrive(canDrive);
        return userMapper.toStoreUser(userRepository.save(user));
    }

    private String toE164(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return null;
        if (phoneNumberProcessor.check(phoneNumber) != PhoneCheck.VALID) throw new AppException(ErrorCode.INVALID_PHONE_NUMBER);
        try {
            return phoneNumberProcessor.toE164(phoneNumber);
        } catch (NumberParseException e) {
            throw new AppException(ErrorCode.INVALID_PHONE_NUMBER);
        }
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
