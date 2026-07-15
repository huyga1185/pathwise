package uk.huy.pathwise.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;
import uk.huy.pathwise.user.dto.request.UserCreationRequest;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.user.mapper.UserMapper;
import uk.huy.pathwise.user.model.User;
import uk.huy.pathwise.user.repository.UserRepository;

import java.util.Optional;

@Service
public class UserService implements UserAccountService, UserProfileService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Override
    public void createUser(String email, String rawPassword) {
        if (userRepository.existsByEmail(email))
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        String hashedPassword = passwordEncoder.encode(rawPassword);
        User user = User.builder()
                .email(email)
                .password(hashedPassword).build();
        userRepository.save(user);
    }

    @Override
    public void updateUser(long id, UserModificationRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COULD_NOT_FIND_USER));
        userMapper.update(user, request);
        userRepository.save(user);
    }

    @Override
    public void updateUserPassword(long id, String newRawPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COULD_NOT_FIND_USER));
        user.setPassword(passwordEncoder.encode(newRawPassword));
        userRepository.save(user);
    }

    @Override
    public void updateUserEmail(long id, String newEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COULD_NOT_FIND_USER));
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COULD_NOT_FIND_USER));
        userRepository.delete(user);
    }

    @Override
    public boolean isUserValid(String email, String password) {
        Optional<User> user = userRepository.findByEmail(email);
        // user ? empty -> do nothing : present -> run {lambda ? true -> present : false -> empty} -> isPresent()
        return user.filter(value -> passwordEncoder.matches(password, value.getPassword()))
                .isPresent();
    }

    @Override
    public GetUserResponse getUser(long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COULD_NOT_FIND_USER));
        return userMapper.toGetUserResponse(user);
    }
}
