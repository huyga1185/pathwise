package uk.huy.pathwise.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.dto.request.RegisterRequest;
import uk.huy.pathwise.auth.dto.request.UpdateUserPasswordRequest;
import uk.huy.pathwise.auth.dto.request.VerifyOtpRequest;
import uk.huy.pathwise.auth.dto.response.LogInResponse;
import uk.huy.pathwise.auth.dto.response.RefreshTokenResponse;
import uk.huy.pathwise.auth.dto.response.SessionResponse;
import uk.huy.pathwise.auth.dto.response.VerifyOtpResponse;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.auth.token.opaque.otptoken.dto.OtpToken;
import uk.huy.pathwise.auth.token.opaque.otptoken.manager.OtpTokenManager;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rejected;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rotated;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.model.SessionRevocation;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.notification.service.EmailDeliveryException;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class AuthService {
    private final UserAccountService userService;
    private final AccessTokenManager accessTokenManager;
    private final RefreshTokenManager refreshTokenManager;
    private final OtpManager otpManager;
    private final OtpTokenManager otpTokenManager;

    // Purpose of the OTP token issued after the OTP is verified (old change-password flow, replaced in T15).
    private static final String UPDATE_USER_PASSWORD_PURPOSE = "updateUserPassword";

    public AuthService(UserAccountService userService,
                       AccessTokenManager accessTokenManager,
                       RefreshTokenManager refreshTokenManager,
                       OtpManager otpManager,
                       OtpTokenManager otpTokenManager) {
        this.userService = userService;
        this.accessTokenManager = accessTokenManager;
        this.refreshTokenManager = refreshTokenManager;
        this.otpManager = otpManager;
        this.otpTokenManager = otpTokenManager;
    }

    public void register(RegisterRequest request) {
        if (request == null) throw new NullPointerException("Register request could not be null");
        userService.createUser(request.email(), request.password());
    }

    public LogInResponse logIn(ClientDetail clientDetail, LogInRequest request) {
        if (request == null) throw new NullPointerException("Log in request could not be null");
        UserIdentity identity = userService.findUserByCredentials(request.email(), request.password())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_EMAIL_OR_PASSWORD));
        RefreshTokenGenerationDetail refreshToken = refreshTokenManager.generateToken(identity.id(), clientDetail);
        return new LogInResponse(accessTokenManager.generateAccessToken(identity, refreshToken.sessionId()),
                refreshToken,
                identity);
    }

    public RefreshTokenResponse refreshToken(String refreshToken, ClientDetail clientDetail) {
        Rotated rotated = switch (refreshTokenManager.rotateToken(refreshToken, clientDetail)) {
            case Rotated r -> r;
            case Rejected r -> {
                // Reuse is already logged as a warning with client details by the manager.
                log.info("Refresh token rejected: {}", r.reason());
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }
        };
        // Only "user still exists" for now; users get a status (ACTIVE/DISABLED) in T13.
        UserIdentity identity = userService.findUserById(rotated.userId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));
        return new RefreshTokenResponse(accessTokenManager.generateAccessToken(identity, rotated.sessionId()), rotated);
    }

    public void logOut(String rawToken) {
        refreshTokenManager.revokeToken(rawToken);
    }

    public List<SessionResponse> listSessions(AuthenticatedUser actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        return refreshTokenManager.listSessions(actor.identity().id()).stream()
                .map(session -> new SessionResponse(session.sessionId(),
                        session.userAgent(),
                        session.ipAddress(),
                        session.lastUsedAt(),
                        session.expiresAt(),
                        session.sessionId().equals(actor.sessionId())))
                .toList();
    }

    /**
     * Revokes one session of the caller ({@code DELETE /sessions/{sessionId}}).
     * <p>
     * Two different sessions are involved:
     * <ul>
     *   <li>{@code actor}: who is calling. {@code actor.identity().id()} is the owner the target must belong to, and
     *       {@code actor.sessionId()} is the session the caller is using right now.</li>
     *   <li>{@code targetSessionId}: the session to revoke, taken from the URL; any active session of the caller.</li>
     * </ul>
     * If the target <b>is</b> the caller's current session, this is a log-out of this device, exactly like
     * {@code /log-out} (the refresh token family is revoked the same way), and the result tells the controller to
     * answer like {@code /log-out} (clear the web cookie). The client chose a session marked {@code current} in
     * {@code GET /sessions}, so it must then drop its tokens, as after {@code /log-out}. An access token issued
     * before T11 has no {@code sid}: no session is "current" and the target is treated as another session.
     *
     * @throws AppException {@code SESSION_NOT_FOUND} if the caller has no such active session: unknown id, another
     *                      user's session (404, not 403, so its existence is not revealed), already revoked or expired
     */
    public SessionRevocation revokeSession(AuthenticatedUser actor, String targetSessionId) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (!refreshTokenManager.revokeSession(actor.identity().id(), targetSessionId))
            throw new AppException(ErrorCode.SESSION_NOT_FOUND);
        return targetSessionId.equals(actor.sessionId())
                ? SessionRevocation.CURRENT_SESSION_LOGGED_OUT
                : SessionRevocation.OTHER_SESSION_REVOKED;
    }

    /**
     * Keeps only the caller's session.
     */
    public void revokeOtherSessions(AuthenticatedUser actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        refreshTokenManager.revokeAllForUser(actor.identity().id(), actor.sessionId());
    }

    public void getUpdateUserPasswordToken(UserIdentity identity) {
        if (identity == null) throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        OtpSendResult result;
        try {
            result = otpManager.sendOtp(OtpPurpose.CHANGE_PASSWORD, identity.id(), identity.email());
        } catch (EmailDeliveryException e) {
            throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        }
        if (result == OtpSendResult.COOLDOWN) throw new AppException(ErrorCode.OTP_COOLDOWN);
    }

    public VerifyOtpResponse verifyUpdateUserPasswordOtp(UserIdentity identity, VerifyOtpRequest request) {
        if (!otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, identity.id(), request.otp()))
            throw new AppException(ErrorCode.INVALID_OTP);
        OtpToken token = otpTokenManager.generateToken(UPDATE_USER_PASSWORD_PURPOSE, identity.id());
        return new VerifyOtpResponse(token.token(), token.ticket(), token.expiresAfter());
    }

    public void updateUserPassword(UserIdentity identity, String ticket, String token, UpdateUserPasswordRequest request) {
        if (ticket == null || ticket.isEmpty()) throw new AppException(ErrorCode.COULD_NOT_UPDATE_USER_PASSWORD);
        if (token == null || token.isEmpty()) throw new AppException(ErrorCode.INVALID_TOKEN);
        Optional<Long> tokenUserId = otpTokenManager.verifyOtpToken(ticket, token, UPDATE_USER_PASSWORD_PURPOSE);
        if (tokenUserId.filter(userId -> userId == identity.id()).isEmpty())
            throw new AppException(ErrorCode.INVALID_TOKEN);
        userService.updateUserPassword(identity.id(), request.password());
    }
}
