package uk.huy.pathwise.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.dto.response.LogInResponse;
import uk.huy.pathwise.auth.dto.response.RefreshTokenResponse;
import uk.huy.pathwise.auth.dto.response.SessionResponse;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rejected;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rotated;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.model.SessionRevocation;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.dto.AuthenticationResult.Authenticated;
import uk.huy.pathwise.user.dto.AuthenticationResult.Disabled;
import uk.huy.pathwise.user.dto.AuthenticationResult.EmailNotVerified;
import uk.huy.pathwise.user.dto.AuthenticationResult.InvalidCredentials;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.List;

@Service
@Slf4j
public class AuthService {
    private final UserAccountService userService;
    private final AccessTokenManager accessTokenManager;
    private final RefreshTokenManager refreshTokenManager;

    public AuthService(UserAccountService userService,
                       AccessTokenManager accessTokenManager,
                       RefreshTokenManager refreshTokenManager) {
        this.userService = userService;
        this.accessTokenManager = accessTokenManager;
        this.refreshTokenManager = refreshTokenManager;
    }

    /**
     * @throws AppException {@code INVALID_EMAIL_OR_PASSWORD} (401), or for a correct password on an account that
     *                      cannot log in (BR-04) {@code EMAIL_NOT_VERIFIED} or {@code ACCOUNT_DISABLED} (403)
     */
    public LogInResponse logIn(ClientDetail clientDetail, LogInRequest request) {
        if (request == null) throw new NullPointerException("Log in request could not be null");
        Authenticated user = switch (userService.authenticate(request.email(), request.password())) {
            case Authenticated authenticated -> authenticated;
            case InvalidCredentials ignored -> throw new AppException(ErrorCode.INVALID_EMAIL_OR_PASSWORD);
            case EmailNotVerified ignored -> throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
            case Disabled ignored -> throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        };
        RefreshTokenGenerationDetail refreshToken = refreshTokenManager.generateToken(user.identity().id(), clientDetail);
        return new LogInResponse(accessTokenManager.generateAccessToken(user.identity(), refreshToken.sessionId()),
                refreshToken,
                user);
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
        // A user deleted, disabled or not yet verified gets no new access token (BR-04).
        UserIdentity identity = userService.findActiveUserById(rotated.userId())
                .orElseThrow(() -> {
                    log.info("Refresh rejected, user missing or not active. userId={}", rotated.userId());
                    return new AppException(ErrorCode.INVALID_TOKEN);
                });
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
     * @throws AppException {@code NOT_FOUND} if the caller has no such active session: unknown id, another
     *                      user's session (404, not 403, so its existence is not revealed), already revoked or expired
     */
    public SessionRevocation revokeSession(AuthenticatedUser actor, String targetSessionId) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (!refreshTokenManager.revokeSession(actor.identity().id(), targetSessionId))
            throw new AppException(ErrorCode.NOT_FOUND);
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
}
