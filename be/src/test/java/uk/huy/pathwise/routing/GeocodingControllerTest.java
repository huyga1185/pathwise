package uk.huy.pathwise.routing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.auth.infrastructure.config.SecurityConfig;
import uk.huy.pathwise.auth.token.jwt.dto.AccessTokenVerification;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.routing.controller.GeocodingController;
import uk.huy.pathwise.routing.dto.response.PlaceResponse;
import uk.huy.pathwise.routing.service.GeocodingService;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeocodingController.class)
@Import(SecurityConfig.class)
class GeocodingControllerTest {
    private static final String DISPATCHER = "Bearer dispatcher-token";
    private static final String DRIVER = "Bearer driver-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeocodingService geocodingService;
    @MockitoBean
    private AccessTokenManager accessTokenManager;

    @BeforeEach
    void setUp() {
        when(accessTokenManager.verifyAccessToken("dispatcher-token")).thenReturn(authenticated(UserRole.DISPATCHER));
        when(accessTokenManager.verifyAccessToken("driver-token")).thenReturn(authenticated(UserRole.DRIVER));
    }

    @Test
    void search_whenValid_returnPlacesWithDefaultLimit5() throws Exception {
        when(geocodingService.search("12 Lê Duẩn", 5)).thenReturn(List.of(new PlaceResponse("12 Lê Duẩn, Đà Nẵng", 16.07, 108.22)));

        mockMvc.perform(get("/api/v1/geocoding/search").param("q", " 12 Lê Duẩn ").header(HttpHeaders.AUTHORIZATION, DISPATCHER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].displayName").value("12 Lê Duẩn, Đà Nẵng"))
                .andExpect(jsonPath("$.data[0].lat").value(16.07))
                .andExpect(jsonPath("$.data[0].lon").value(108.22));
    }

    @Test
    void search_whenQueryTooShortOrTooLong_return400() throws Exception {
        mockMvc.perform(get("/api/v1/geocoding/search").param("q", "ab").header(HttpHeaders.AUTHORIZATION, DISPATCHER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].code").value("INVALID_QUERY_LENGTH"));
        mockMvc.perform(get("/api/v1/geocoding/search").param("q", "x".repeat(301)).header(HttpHeaders.AUTHORIZATION, DISPATCHER))
                .andExpect(jsonPath("$.details[0].code").value("INVALID_QUERY_LENGTH"));
        verifyNoInteractions(geocodingService);
    }

    @Test
    void search_whenLimitOutOfRange_return400() throws Exception {
        mockMvc.perform(get("/api/v1/geocoding/search").param("q", "Đà Nẵng").param("limit", "11").header(HttpHeaders.AUTHORIZATION, DISPATCHER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].code").value("INVALID_LIMIT"));
        mockMvc.perform(get("/api/v1/geocoding/search").param("q", "Đà Nẵng").param("limit", "0").header(HttpHeaders.AUTHORIZATION, DISPATCHER))
                .andExpect(jsonPath("$.details[0].code").value("INVALID_LIMIT"));
    }

    @Test
    void search_whenQueryMissing_return400MalformedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/geocoding/search").header(HttpHeaders.AUTHORIZATION, DISPATCHER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void search_whenProviderFails_return502() throws Exception {
        when(geocodingService.search(anyString(), anyInt()))
                .thenThrow(new ExternalServiceException(ErrorCode.GEOCODING_PROVIDER_ERROR, "down", null));

        mockMvc.perform(get("/api/v1/geocoding/search").param("q", "Đà Nẵng").header(HttpHeaders.AUTHORIZATION, DISPATCHER))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("GEOCODING_PROVIDER_ERROR"));
    }

    @Test
    void search_whenRateLimitBusy_return503() throws Exception {
        when(geocodingService.search(anyString(), anyInt()))
                .thenThrow(new ExternalServiceException(ErrorCode.GEOCODING_BUSY, "busy", null));

        mockMvc.perform(get("/api/v1/geocoding/search").param("q", "Đà Nẵng").header(HttpHeaders.AUTHORIZATION, DISPATCHER))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("GEOCODING_BUSY"));
    }

    @Test
    void search_whenDriverOrAnonymous_rejected() throws Exception {
        mockMvc.perform(get("/api/v1/geocoding/search").param("q", "Đà Nẵng").header(HttpHeaders.AUTHORIZATION, DRIVER))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/geocoding/search").param("q", "Đà Nẵng"))
                .andExpect(status().isUnauthorized());
    }

    private static AccessTokenVerification authenticated(UserRole role) {
        return new AccessTokenVerification.Authenticated(
                new AuthenticatedUser(new UserIdentity(1L, "u@pathwise.test", role, 1L, false), "s"));
    }
}
