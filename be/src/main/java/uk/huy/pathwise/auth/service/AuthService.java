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
import uk.huy.pathwise.auth.email.manager.OtpEmailManager;
import uk.huy.pathwise.auth.otp.dto.Otp;
import uk.huy.pathwise.auth.otp.infrastructure.exception.OtpException;
import uk.huy.pathwise.auth.otp.store.OtpStore;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.auth.token.opaque.otptoken.dto.OtpToken;
import uk.huy.pathwise.auth.token.opaque.otptoken.manager.OtpTokenManager;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenRotationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.Optional;

@Service
public class AuthService {
    private final UserAccountService userService;
    private final AccessTokenManager accessTokenManager;
    private final RefreshTokenManager refreshTokenManager;
    private final OtpStore otpManager;
    private final OtpTokenManager otpTokenManager;
    private final OtpEmailManager otpEmailManager;

    private static final String UPDATE_USER_PASSWORD_PURPOSE = "updateUserPassword";

    public AuthService(UserAccountService userService,
                       AccessTokenManager accessTokenManager,
                       RefreshTokenManager refreshTokenManager,
                       OtpStore otpManager,
                       OtpTokenManager otpTokenManager,
                       OtpEmailManager otpEmailManager) {
        this.userService = userService;
        this.accessTokenManager = accessTokenManager;
        this.refreshTokenManager = refreshTokenManager;
        this.otpManager = otpManager;
        this.otpTokenManager = otpTokenManager;
        this.otpEmailManager = otpEmailManager;
    }

    public void register(RegisterRequest request) {
        if (request == null) throw new NullPointerException("Register request could not be null");
        userService.createUser(request.email(), request.password());
    }

    public LogInResponse logIn(ClientDetail clientDetail, LogInRequest request) {
        if (request == null) throw new NullPointerException("Log in request could not be null");
        Optional<UserIdentity> identity = userService.findUserByCredentials(request.email(), request.password());
        if (identity.isEmpty()) throw new AppException(ErrorCode.INVALID_EMAIL_OR_PASSWORD);
        return new LogInResponse(accessTokenManager.generateAccessToken(identity.get()),
                refreshTokenManager.generateToken(identity.get().id(), clientDetail),
                identity.get());
    }

    public RefreshTokenResponse refreshToken(String refreshToken, ClientDetail clientDetail) {
        Optional<RefreshTokenRotationDetail> response = refreshTokenManager.rotateToken(refreshToken, clientDetail);
        if (response.isEmpty()) throw new AppException(ErrorCode.INVALID_TOKEN);
        RefreshTokenRotationDetail refreshTokenRotationDetail = response.get();
        // Only "user still exists" for now; users get a status (ACTIVE/DISABLED) in T13.
        Optional<UserIdentity> identity = userService.findUserById(refreshTokenRotationDetail.userId());
        if (identity.isEmpty()) throw new AppException(ErrorCode.INVALID_TOKEN);
        return new RefreshTokenResponse(accessTokenManager.generateAccessToken(identity.get()), refreshTokenRotationDetail);
    }

    public void logOut(String rawToken) {
        refreshTokenManager.revokeToken(rawToken);
    }

    public GetListSessionResponse getListSession(UserIdentity userIdentity) {
        if (userIdentity == null) throw new NullPointerException("User identity could not be null");
        return new GetListSessionResponse(refreshTokenManager.getListInfoByUserId(userIdentity.id()));
    }

    public void getUpdateUserPasswordToken(UserIdentity identity) {
        if (identity == null) throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        Otp otp;
        try {
            otp = otpManager.generateAndStoreOtp(UPDATE_USER_PASSWORD_PURPOSE, identity.email());
        } catch (OtpException e) {
            throw new AppException(ErrorCode.OTP_COOLDOWN);
        }
        try {
            otpEmailManager.send(identity.email(), otp.otp());
        } catch (EmailSenderException e) {
            throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        }
    }

    public VerifyOtpResponse verifyUpdateUserPasswordOtp(UserIdentity identity, VerifyOtpRequest request) {
        if (!otpManager.verifyOtp(request.otp(), UPDATE_USER_PASSWORD_PURPOSE, String.valueOf(identity.email())))
            throw new AppException(ErrorCode.INVALID_OTP);
        OtpToken token = otpTokenManager.generateToken(UPDATE_USER_PASSWORD_PURPOSE, identity.id());
        return new VerifyOtpResponse(token.token(), token.ticket(), token.expiresAfter());
    }

    public void updateUserPassword(UserIdentity identity, String ticket, String token, UpdateUserPasswordRequest request) {
        if (ticket == null || ticket.isEmpty()) throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        if (token == null || token.isEmpty()) throw new AppException(ErrorCode.INVALID_TOKEN);
        Optional<Long> tokenUserId = otpTokenManager.verifyOtpToken(ticket, token, UPDATE_USER_PASSWORD_PURPOSE);
        if (tokenUserId.isEmpty() || tokenUserId.get() != identity.id())
            throw new AppException(ErrorCode.INVALID_TOKEN);
        userService.updateUserPassword(identity.id(), request.password());
    }
}
