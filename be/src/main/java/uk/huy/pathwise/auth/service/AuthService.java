package uk.huy.pathwise.auth.service;

import com.nimbusds.jose.JOSEException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.dto.request.RegisterRequest;
import uk.huy.pathwise.auth.dto.response.LogInResponse;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.infastructure.token.jwt.JwtService;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserAccountService userService;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {
        userService.createUser(request.email(), request.password());
    }

    public LogInResponse logIn(ClientDetail clientDetail, LogInRequest request) throws JOSEException {
        Optional<UserIdentity> identity = userService.findUserByCredentials(request.email(), request.password());
        if (identity.isEmpty()) throw new AppException(ErrorCode.INVALID_EMAIL_OR_PASSWORD);
        return new LogInResponse(jwtService.generateJwt(identity.get()));
    }
}
