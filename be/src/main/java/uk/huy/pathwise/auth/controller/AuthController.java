package uk.huy.pathwise.auth.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uk.huy.pathwise.auth.dto.request.UpdateUserPasswordRequest;
import uk.huy.pathwise.auth.dto.request.VerifyOtpRequest;
import uk.huy.pathwise.auth.dto.response.GetListSessionResponse;
import uk.huy.pathwise.auth.dto.response.RefreshTokenResponse;
import uk.huy.pathwise.auth.dto.response.VerifyOtpResponse;
import uk.huy.pathwise.auth.infastructure.annotation.CurrentClient;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.dto.request.RegisterRequest;
import uk.huy.pathwise.auth.dto.response.LogInResponse;
import uk.huy.pathwise.auth.infastructure.builder.AuthCookieBuilder;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.service.AuthService;
import uk.huy.pathwise.core.apiresponse.ApiResponse;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthCookieBuilder authCookieBuilder;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String UPDATE_USER_PASSWORD_COOKIE = "updateUserPassword";

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/log-in")
    public ResponseEntity<ApiResponse<Map<String, String>>> logIn(@CurrentClient ClientDetail clientDetail,
                                                                  @RequestBody LogInRequest request) {
        LogInResponse logInResponse = authService.logIn(clientDetail, request);
        ResponseCookie cookie = authCookieBuilder.createSecureCookie(REFRESH_TOKEN_COOKIE,
                logInResponse.refreshToken().token(),
                "/auth/refresh",
                logInResponse.refreshToken().expiresAt());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new ApiResponse<>(null, Map.of("accessToken", logInResponse.accessToken())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<String>> refreshToken(@CookieValue(REFRESH_TOKEN_COOKIE) String refreshToken,
                                                            @CurrentClient ClientDetail clientDetail) {
        RefreshTokenResponse response = authService.refreshToken(refreshToken, clientDetail);
        ResponseCookie cookie = authCookieBuilder.createSecureCookie(REFRESH_TOKEN_COOKIE,
                response.refreshTokenRotationDetail().token(),
                "/auth/refresh",
                response.refreshTokenRotationDetail().expiresAt());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new ApiResponse<>(response.accessToken()));
    }

    @PostMapping("/log-out")
    public ResponseEntity<Void> logOut(@CookieValue("refreshToken") String refreshToken) {
        authService.logOut(refreshToken);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE, authCookieBuilder.removeSecureCookie(REFRESH_TOKEN_COOKIE, "removed", "/auth/refresh").toString())
                .build();
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
                                                        @RequestBody VerifyOtpRequest verifyOtpRequest) {
        VerifyOtpResponse verifyOtpResponse = authService.verifyUpdateUserPasswordOtp(identity, verifyOtpRequest);
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE,
                        authCookieBuilder.createSecureCookie(UPDATE_USER_PASSWORD_COOKIE,
                                verifyOtpResponse.token(),
                                "/auth/update-password",
                                verifyOtpResponse.expiresAt()).toString())
                .build();
    }

    @PostMapping("/update-password")
    public ResponseEntity<Void> updateUserPassword(@CookieValue(UPDATE_USER_PASSWORD_COOKIE) String token,
                                                   @RequestBody UpdateUserPasswordRequest request,
                                                   @AuthenticationPrincipal UserIdentity identity) {
        authService.updateUserPassword(identity, token, request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.SET_COOKIE,
                        authCookieBuilder.removeSecureCookie(UPDATE_USER_PASSWORD_COOKIE,
                                null,
                                "/auth/update-password").toString())
                .build();
    }
}
