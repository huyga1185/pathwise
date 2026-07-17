package uk.huy.pathwise.routeoptimizer.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationMultiVehicleRequest;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationRequest;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationMultiVehicleResponse;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationResponse;
import uk.huy.pathwise.routeoptimizer.service.OptimizationService;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OptimizationController.class)
class OptimizationControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OptimizationService optimizationService;

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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorCode.INVALID_DEPOT.name()));
    }
}
