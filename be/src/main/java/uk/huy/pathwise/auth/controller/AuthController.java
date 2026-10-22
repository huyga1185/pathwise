package uk.huy.pathwise.auth.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.auth.dto.request.RefreshTokenRequest;
import uk.huy.pathwise.auth.dto.request.UpdateUserPasswordRequest;
import uk.huy.pathwise.auth.dto.request.VerifyOtpRequest;
import uk.huy.pathwise.auth.dto.response.AuthTokenResponse;
import uk.huy.pathwise.auth.dto.response.AuthUserResponse;
import uk.huy.pathwise.auth.dto.response.GetListSessionResponse;
import uk.huy.pathwise.auth.dto.response.RefreshTokenResponse;
import uk.huy.pathwise.auth.dto.response.VerifyOtpResponse;
import uk.huy.pathwise.auth.infrastructure.annotation.CurrentClient;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.dto.request.RegisterRequest;
import uk.huy.pathwise.auth.dto.response.LogInResponse;
import uk.huy.pathwise.auth.infrastructure.builder.AuthCookieBuilder;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.service.AuthService;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenRotationDetail;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
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
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/log-in")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> logIn(@CurrentClient ClientDetail clientDetail,
                                                                @RequestHeader(value = CLIENT_PLATFORM_HEADER, required = false) String platform,
                                                                @Valid @RequestBody LogInRequest request) {
        LogInResponse logInResponse = authService.logIn(clientDetail, request);
        boolean mobile = isMobile(platform);
        UserIdentity user = logInResponse.user();
        AuthTokenResponse body = new AuthTokenResponse(logInResponse.accessToken().token(),
                logInResponse.accessToken().expiresAt(),
                mobile ? logInResponse.refreshToken().token() : null,
                logInResponse.refreshToken().expiresAt(),
                new AuthUserResponse(user.id(), user.email(), null, user.role().name(), null, null));
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
        RefreshTokenRotationDetail rotated = response.refreshTokenRotationDetail();
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
        ResponseEntity.HeadersBuilder<?> builder = ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache");
        if (!mobile) {
            builder.header(HttpHeaders.SET_COOKIE,
                    authCookieBuilder.removeSecureCookie(REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_COOKIE_PATH).toString());
        }
        return builder.build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<GetListSessionResponse>> getListSession(@AuthenticationPrincipal UserIdentity userIdentity) {
        return ResponseEntity.ok()
                .body(new ApiResponse<>(authService.getListSession(userIdentity)));
    }

    @GetMapping("/update-password")
    public ResponseEntity<Void> getUserPasswordToken(@AuthenticationPrincipal UserIdentity identity) {
        authService.getUpdateUserPasswordToken(identity);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/update-password/verify")
    public ResponseEntity<Void> verifyUserPasswordToken(@AuthenticationPrincipal UserIdentity identity,
                                                        @Valid @RequestBody VerifyOtpRequest verifyOtpRequest) {
        VerifyOtpResponse verifyOtpResponse = authService.verifyUpdateUserPasswordOtp(identity, verifyOtpRequest);
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
                                                   @AuthenticationPrincipal UserIdentity identity) {
        authService.updateUserPassword(identity, ticket, token, request);
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
