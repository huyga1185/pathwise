package uk.huy.pathwise.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.auth.dto.request.RefreshTokenRequest;
import uk.huy.pathwise.auth.dto.request.ResendVerificationRequest;
import uk.huy.pathwise.auth.dto.request.VerifyEmailRequest;
import uk.huy.pathwise.auth.dto.response.RegisterResponse;
import uk.huy.pathwise.auth.dto.request.UpdateUserPasswordRequest;
import uk.huy.pathwise.auth.dto.request.VerifyOtpRequest;
import uk.huy.pathwise.auth.dto.response.AuthTokenResponse;
import uk.huy.pathwise.auth.dto.response.AuthUserResponse;
import uk.huy.pathwise.auth.dto.response.SessionResponse;
import uk.huy.pathwise.auth.dto.response.RefreshTokenResponse;
import uk.huy.pathwise.auth.dto.response.VerifyOtpResponse;
import uk.huy.pathwise.auth.infrastructure.annotation.CurrentClient;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.dto.request.RegisterRequest;
import uk.huy.pathwise.auth.dto.response.LogInResponse;
import uk.huy.pathwise.auth.infrastructure.builder.AuthCookieBuilder;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.service.AuthService;
import uk.huy.pathwise.auth.service.RegistrationService;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RegistrationService registrationService;
    private final AuthCookieBuilder authCookieBuilder;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    // Covers /refresh and /log-out (F-03).
    private static final String REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth";
    private static final String CLIENT_PLATFORM_HEADER = "X-Client-Platform";
    private static final String MOBILE_PLATFORM = "mobile";
    private final String UPDATE_USER_PASSWORD_ROUTE = "/api/v1/auth/update-password";
    private final String UPDATE_USER_PASSWORD_COOKIE = "updateUserPassword";
    private final String UPDATE_USER_PASSWORD_TICKET = "updateUserPasswordTicket";

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(registrationService.register(request)));
    }

    @PostMapping("/email/verify")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        registrationService.verifyEmail(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Always 204, whether or not the email is registered.
     */
    @PostMapping("/email/verify/resend")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        registrationService.resendVerification(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/log-in")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> logIn(@CurrentClient ClientDetail clientDetail,
                                                                @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                                                @Valid @RequestBody LogInRequest request) {
        LogInResponse logInResponse = authService.logIn(clientDetail, request);
        boolean mobile = isMobile(platform);
        UserIdentity user = logInResponse.user().identity();
        AuthTokenResponse body = new AuthTokenResponse(logInResponse.accessToken().token(),
                logInResponse.accessToken().expiresAt(),
                mobile ? logInResponse.refreshToken().token() : null,
                logInResponse.refreshToken().expiresAt(),
                new AuthUserResponse(user.id(),
                        user.email(),
                        logInResponse.user().fullName(),
                        user.role().name(),
                        user.storeId(),
                        user.assignableToRoutes()));
        return tokenResponse(body, mobile, logInResponse.refreshToken().token(), logInResponse.refreshToken().expiresAt());
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refreshToken(@CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String cookieToken,
                                                                       @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                                                       @RequestBody(required = false) RefreshTokenRequest request,
                                                                       @CurrentClient ClientDetail clientDetail) {
        boolean mobile = isMobile(platform);
        String rawToken = mobile ? (request != null ? request.refreshToken() : null) : cookieToken;
        // Missing credential: same 401 as an unknown token, whatever the transport.
        if (rawToken == null || rawToken.isBlank()) throw new AppException(ErrorCode.INVALID_TOKEN);
        RefreshTokenResponse response = authService.refreshToken(rawToken, clientDetail);
        RotationResult.Rotated rotated = response.refreshToken();
        AuthTokenResponse body = new AuthTokenResponse(response.accessToken().token(),
                response.accessToken().expiresAt(),
                mobile ? rotated.token() : null,
                rotated.expiresAt(),
                null);
        return tokenResponse(body, mobile, rotated.token(), rotated.expiresAt());
    }

    /**
     * Public and idempotent (P-04): the refresh token identifies the session, so an expired access token
     * must not prevent logging out. Always 204.
     */
    @PostMapping("/log-out")
    public ResponseEntity<Void> logOut(@CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String cookieToken,
                                       @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                       @RequestBody(required = false) RefreshTokenRequest request) {
        boolean mobile = isMobile(platform);
        authService.logOut(mobile ? (request != null ? request.refreshToken() : null) : cookieToken);
        return loggedOutResponse(mobile);
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> listSessions(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(new ApiResponse<>(authService.listSessions(actor)));
    }

    /**
     * {@code actor} is the caller (its own session is in {@code actor.sessionId()}); the path variable is the session
     * to revoke. Revoking the caller's current session is a log-out and gets the same response as {@code /log-out}.
     * See {@link AuthService#revokeSession}.
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> revokeSession(@AuthenticationPrincipal AuthenticatedUser actor,
                                              @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                              @PathVariable("sessionId") String targetSessionId) {
        return switch (authService.revokeSession(actor, targetSessionId)) {
            case CURRENT_SESSION_LOGGED_OUT -> loggedOutResponse(isMobile(platform));
            case OTHER_SESSION_REVOKED -> ResponseEntity.noContent().build();
        };
    }

    @PostMapping("/sessions/revoke-others")
    public ResponseEntity<Void> revokeOtherSessions(@AuthenticationPrincipal AuthenticatedUser actor) {
        authService.revokeOtherSessions(actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/update-password")
    public ResponseEntity<Void> getUserPasswordToken(@AuthenticationPrincipal AuthenticatedUser actor) {
        authService.getUpdateUserPasswordToken(actor.identity());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/update-password/verify")
    public ResponseEntity<Void> verifyUserPasswordToken(@AuthenticationPrincipal AuthenticatedUser actor,
                                                        @Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        VerifyOtpResponse verifyOtpResponse = authService.verifyUpdateUserPasswordOtp(actor.identity(), verifyOtpRequest);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE,
                        authCookieBuilder.createSecureCookie(UPDATE_USER_PASSWORD_COOKIE,
                                verifyOtpResponse.token(),
                                UPDATE_USER_PASSWORD_ROUTE,
                                verifyOtpResponse.expiresAfter()).toString())
                .header(HttpHeaders.SET_COOKIE,
                        authCookieBuilder.createSecureCookie(UPDATE_USER_PASSWORD_TICKET,
                                verifyOtpResponse.ticket(),
                                UPDATE_USER_PASSWORD_ROUTE,
                                verifyOtpResponse.expiresAfter()).toString())
                .build();
    }

    @PostMapping("/update-password")
    public ResponseEntity<Void> updateUserPassword(@CookieValue(UPDATE_USER_PASSWORD_COOKIE) String token,
                                                   @CookieValue(UPDATE_USER_PASSWORD_TICKET) String ticket,
                                                   @Valid @RequestBody UpdateUserPasswordRequest request,
                                                   @AuthenticationPrincipal AuthenticatedUser actor) {
        authService.updateUserPassword(actor.identity(), ticket, token, request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE,
                        authCookieBuilder.removeSecureCookie(UPDATE_USER_PASSWORD_COOKIE,
                                null,
                                UPDATE_USER_PASSWORD_ROUTE).toString())
                .header(HttpHeaders.SET_COOKIE,
                        authCookieBuilder.removeSecureCookie(UPDATE_USER_PASSWORD_TICKET,
                                null,
                                UPDATE_USER_PASSWORD_ROUTE).toString())
                .build();
    }

    /**
     * Response of a log-out of this device: 204, not cached, and for web the refresh cookie is cleared.
     */
    private ResponseEntity<Void> loggedOutResponse(boolean mobile) {
        ResponseEntity.HeadersBuilder<?> builder = ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache");
        if (!mobile) {
            builder.header(HttpHeaders.SET_COOKIE,
                    authCookieBuilder.removeSecureCookie(REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_COOKIE_PATH).toString());
        }
        return builder.build();
    }

    private static boolean isMobile(String platform) {
        return MOBILE_PLATFORM.equalsIgnoreCase(platform);
    }

    /**
     * Web clients get the refresh token only as an HttpOnly cookie; mobile clients only in the body (D-14, P-04).
     */
    private ResponseEntity<ApiResponse<AuthTokenResponse>> tokenResponse(AuthTokenResponse body,
                                                                         boolean mobile,
                                                                         String refreshToken,
                                                                         Instant refreshTokenExpiresAt) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache");
        if (!mobile) {
            builder.header(HttpHeaders.SET_COOKIE, authCookieBuilder.createSecureCookie(REFRESH_TOKEN_COOKIE,
                    refreshToken, REFRESH_TOKEN_COOKIE_PATH, refreshTokenExpiresAt).toString());
        }
        return builder.body(new ApiResponse<>(body));
    }
}
