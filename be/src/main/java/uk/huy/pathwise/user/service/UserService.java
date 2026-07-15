package uk.huy.pathwise.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;
import uk.huy.pathwise.user.dto.request.UserCreationRequest;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.model.User;
import uk.huy.pathwise.user.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void createUser(UserCreationRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        String hashedPassword = passwordEncoder.encode(request.password());
        userRepository.save(User.builder()
                .email(request.email())
                .password(hashedPassword)
                .build());
    }

    public void updateUser(UserModificationRequest request) {

    }

    public void deleteUser() {

    }
}
