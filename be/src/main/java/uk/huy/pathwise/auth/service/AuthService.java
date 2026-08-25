package uk.huy.pathwise.auth.service;

import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.dto.request.RegisterRequest;
import uk.huy.pathwise.auth.dto.request.UpdateUserPasswordRequest;
import uk.huy.pathwise.auth.dto.request.VerifyOtpRequest;
import uk.huy.pathwise.auth.dto.response.GetListSessionResponse;
import uk.huy.pathwise.auth.dto.response.LogInResponse;
import uk.huy.pathwise.auth.dto.response.RefreshTokenResponse;
import uk.huy.pathwise.auth.dto.response.VerifyOtpResponse;
import uk.huy.pathwise.auth.email.infrastructure.exception.EmailSenderException;
import uk.huy.pathwise.auth.email.service.OtpEmailService;
import uk.huy.pathwise.auth.otp.infrastructure.exception.OtpException;
import uk.huy.pathwise.auth.otp.OtpService;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.GeneratedOtpToken;
import uk.huy.pathwise.auth.token.jwt.service.AccessTokenService;
import uk.huy.pathwise.auth.token.jwt.service.OtpTokenService;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenRotationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.service.RefreshTokenService;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.Optional;

@Service
public class AuthService {
    private final UserAccountService userService;
    private final AccessTokenService accessTokenService;
    private final RefreshTokenService refreshTokenService;
    private final OtpService otpService;
    private final OtpTokenService otpTokenService;
    private final OtpEmailService otpMailService;

    private static final String UPDATE_USER_PASSWORD_PURPOSE = "updateUserPassword";

    public AuthService(UserAccountService userService,
                       AccessTokenService accessTokenService,
                       RefreshTokenService refreshTokenService,
                       OtpService otpService,
                       OtpTokenService otpTokenService,
                       OtpEmailService otpMailService) {
        this.userService = userService;
        this.accessTokenService = accessTokenService;
        this.refreshTokenService = refreshTokenService;
        this.otpService = otpService;
        this.otpTokenService = otpTokenService;
        this.otpMailService = otpMailService;
    }

    public void register(RegisterRequest request) {
        if (request == null) throw new NullPointerException("Register request could not be null");
        userService.createUser(request.email(), request.password());
    }

    public LogInResponse logIn(ClientDetail clientDetail, LogInRequest request) {
        if (request == null) throw new NullPointerException("Log in request could not be null");
        Optional<UserIdentity> identity = userService.findUserByCredentials(request.email(), request.password());
        if (identity.isEmpty()) throw new AppException(ErrorCode.INVALID_EMAIL_OR_PASSWORD);
        return new LogInResponse(accessTokenService.generateAccessToken(identity.get()), refreshTokenService.generateToken(identity.get().id(), clientDetail));
    }

    public RefreshTokenResponse refreshToken(String refreshToken, ClientDetail clientDetail) {
        Optional<RefreshTokenRotationDetail> response = refreshTokenService.rotateToken(refreshToken, clientDetail);
        if (response.isEmpty()) throw new AppException(ErrorCode.INVALID_TOKEN);
        RefreshTokenRotationDetail refreshTokenRotationDetail = response.get();
        Optional<UserIdentity> identity = userService.findUserById(refreshTokenRotationDetail.userId());
        if (identity.isEmpty()) throw new AppException(ErrorCode.INVALID_TOKEN);
        String accessToken = accessTokenService.generateAccessToken(identity.get());
        return new RefreshTokenResponse(accessToken, refreshTokenRotationDetail);
    }

    public void logOut(String rawToken) {
        refreshTokenService.revokeToken(rawToken);
    }

    public GetListSessionResponse getListSession(UserIdentity userIdentity) {
        if (userIdentity == null) throw new NullPointerException("User identity could not be null");
        return new GetListSessionResponse(refreshTokenService.getListInfoByUserId(userIdentity.id()));
    }

    public void getUpdateUserPasswordToken(UserIdentity identity) {
        Optional<String> optionalOtp;
        try {
            optionalOtp = otpService.generateAndStoreOtp(UPDATE_USER_PASSWORD_PURPOSE, String.valueOf(identity.id()));
        } catch (OtpException e) {
            throw new AppException(ErrorCode.OTP_COOLDOWN);
        }
        if (optionalOtp.isEmpty()) throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        String otp = optionalOtp.get();
        try {
            otpMailService.send(identity.email(), otp);
        } catch (EmailSenderException e) {
            throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        }
    }

    public VerifyOtpResponse verifyUpdateUserPasswordOtp(UserIdentity identity, VerifyOtpRequest request) {
        if (!otpService.verifyOtp(request.otp(), UPDATE_USER_PASSWORD_PURPOSE, String.valueOf(identity.id())))
            throw new AppException(ErrorCode.INVALID_OTP);
        GeneratedOtpToken token = otpTokenService.generateOtpToken(identity, UPDATE_USER_PASSWORD_PURPOSE);
        return new VerifyOtpResponse(token.token(), token.expiresAt());
    }

    public void updateUserPassword(UserIdentity identity, String token, UpdateUserPasswordRequest request) {
        if (request == null) throw new NullPointerException("UpdateUserPasswordRequest could not be null");
        Optional<String> optionalPurpose = otpTokenService.verifyOtpToken(identity, token);
        if (optionalPurpose.isEmpty()) throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        String purpose = optionalPurpose.get();
        if (!purpose.equals(UPDATE_USER_PASSWORD_PURPOSE)) throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        userService.updateUserPassword(identity.id(), request.password());
    }
}
