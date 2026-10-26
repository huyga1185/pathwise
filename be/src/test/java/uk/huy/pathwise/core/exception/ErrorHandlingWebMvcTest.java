package uk.huy.pathwise.core.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.auth.controller.AuthController;
import uk.huy.pathwise.auth.infrastructure.builder.AuthCookieBuilder;
import uk.huy.pathwise.auth.infrastructure.config.SecurityConfig;
import uk.huy.pathwise.auth.service.AuthService;
import uk.huy.pathwise.auth.service.RegistrationService;
import uk.huy.pathwise.auth.token.jwt.dto.AccessTokenVerification;
import uk.huy.pathwise.auth.token.jwt.infrastructure.dto.JwtRejectReason;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.user.controller.AdminUserController;
import uk.huy.pathwise.user.controller.UserController;
import uk.huy.pathwise.user.infrastructure.phonenumber.PhoneNumberProcessor;
import uk.huy.pathwise.user.service.UserProfileService;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error format and HTTP status mapping (API_SPEC §1, §9; ARCHITECTURE §5), exercised through the real
 * security filter chain.
 */
@WebMvcTest({AuthController.class, UserController.class, AdminUserController.class})
@Import({SecurityConfig.class, PhoneNumberProcessor.class})
class ErrorHandlingWebMvcTest {
    private static final String USER_BEARER = "Bearer user-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccessTokenManager accessTokenManager;
    @MockitoBean
    private AuthService authService;
    @MockitoBean
    private RegistrationService registrationService;
    @MockitoBean
    private AuthCookieBuilder authCookieBuilder;
    @MockitoBean
    private UserProfileService userProfileService;

    @BeforeEach
    void setUp() {
        when(accessTokenManager.verifyAccessToken("user-token"))
                .thenReturn(new AccessTokenVerification.Authenticated(new AuthenticatedUser(new UserIdentity(1L, "user@pathwise.test", UserRole.OWNER, 1L), "session-1")));
        when(accessTokenManager.verifyAccessToken("bad-token"))
                .thenReturn(new AccessTokenVerification.Rejected(JwtRejectReason.BAD_SIGNATURE));
    }

    @Test
    void protectedEndpoint_whenNoAccessToken_return401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpoint_whenAccessTokenInvalid_return401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void adminEndpoint_whenRoleIsUser_return403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, USER_BEARER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void register_whenBodyIsMalformedJson_return400MalformedRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void register_whenFieldsInvalid_return400ValidationErrorWithDetails() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[?(@.field == 'email')].code").value("INVALID_EMAIL_FORMAT"))
                .andExpect(jsonPath("$.details[?(@.field == 'password')].code").value("INVALID_PASSWORD_LENGTH"))
                .andExpect(jsonPath("$.details[?(@.field == 'storeName')].code").value("STORE_NAME_REQUIRED"));
    }

    @Test
    void register_whenEmailExisted_return409() throws Exception {
        doThrow(new AppException(ErrorCode.EMAIL_EXISTED)).when(registrationService).register(any());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@pathwise.test\",\"password\":\"password123\",\"storeName\":\"Shop\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTED"));
    }

    @Test
    void register_whenConcurrentInsertViolatesConstraint_return409Conflict() throws Exception {
        doThrow(new DataIntegrityViolationException("Duplicate entry")).when(registrationService).register(any());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@pathwise.test\",\"password\":\"password123\",\"storeName\":\"Shop\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void logIn_whenCredentialsWrong_return401() throws Exception {
        when(authService.logIn(any(), any())).thenThrow(new AppException(ErrorCode.INVALID_EMAIL_OR_PASSWORD));

        mockMvc.perform(post("/api/v1/auth/log-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@pathwise.test\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL_OR_PASSWORD"));
    }

    @Test
    void getProfile_whenUserNotFound_return404() throws Exception {
        when(userProfileService.getUser(any())).thenThrow(new AppException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, USER_BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void unknownPath_whenAuthenticated_return404NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").header(HttpHeaders.AUTHORIZATION, USER_BEARER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void existingPath_whenMethodNotSupported_return405() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, USER_BEARER))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void refresh_whenCookieMissing_return401InvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void updateProfile_whenUnexpectedException_return500WithoutInternals() throws Exception {
        doThrow(new IllegalStateException("boom")).when(userProfileService).updateUser(any(), any());

        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, USER_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":null}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());

        verify(userProfileService).updateUser(any(), any());
    }

    @Test
    void updateProfile_whenPhoneNumberInvalid_return400ValidationError() throws Exception {
        mockMvc.perform(put("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, USER_BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("phoneNumber"));
    }
}
