package uk.huy.pathwise.routeoptimizer.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.auth.infrastructure.config.SecurityConfig;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationMultiVehicleRequest;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationRequest;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationMultiVehicleResponse;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationResponse;
import uk.huy.pathwise.routeoptimizer.service.OptimizationService;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OptimizationController.class)
@Import(SecurityConfig.class)
class OptimizationControllerTest {
    private static final String BEARER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OptimizationService optimizationService;

    @MockitoBean
    private AccessTokenManager accessTokenManager;

    @BeforeEach
    void authenticateTestToken() {
        when(accessTokenManager.verifyAccessToken("test-token"))
                .thenReturn(Optional.of(new UserIdentity(1L, "user@pathwise.test", UserRole.USER)));
    }

    @Test
    void postOptimize_whenNoAccessToken_rejectRequest() throws Exception {
        // Current behaviour without an AuthenticationEntryPoint is 403; T03 changes this to 401 UNAUTHORIZED (F-12).
        mockMvc.perform(post("/route/optimize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"addresses":["a","b"],"depot":0}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void postOptimize_returnsSuccessEnvelopeWithOptimizedRoute() throws Exception {
        String requestJson = """
                {"addresses":["a","b","c"],"depot":0}""";
        OptimizationResponse serviceResult = OptimizationResponse.builder()
                .optimizedRoute(List.of("a", "c", "b"))
                .depot(0)
                .build();
        when(optimizationService.optimizeRoute(any(OptimizationRequest.class)))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/route/optimize")
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.depot").value(0))
                .andExpect(jsonPath("$.data.optimizedRoute[0]").value("a"))
                .andExpect(jsonPath("$.data.optimizedRoute[1]").value("c"))
                .andExpect(jsonPath("$.data.optimizedRoute[2]").value("b"));
    }

    @Test
    void postOptimizeK_returnsSuccessEnvelopeWithPerVehicleRoutes() throws Exception {
        String requestJson = """
                {"addresses":["a","b","c"],"depot":0,"numberVehicle":2}""";
        OptimizationMultiVehicleResponse serviceResult = new OptimizationMultiVehicleResponse(
                List.of(List.of("a", "b", "a"), List.of("a", "c", "a")), 0);
        when(optimizationService.optimizeRouteForKVehicle(any(OptimizationMultiVehicleRequest.class)))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/route/optimize/k")
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.depot").value(0))
                .andExpect(jsonPath("$.data.optimizedRoute[0][1]").value("b"))
                .andExpect(jsonPath("$.data.optimizedRoute[1][1]").value("c"));
    }

    @Test
    void postOptimize_whenServiceThrowsInvalidDepot_returns400WithErrorBody() throws Exception {
        String requestJson = """
                {"addresses":["a","b"],"depot":5}""";
        when(optimizationService.optimizeRoute(any(OptimizationRequest.class)))
                .thenThrow(new AppException(ErrorCode.INVALID_DEPOT));

        mockMvc.perform(post("/route/optimize")
                        .header(HttpHeaders.AUTHORIZATION, BEARER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_DEPOT.name()));
    }
}
