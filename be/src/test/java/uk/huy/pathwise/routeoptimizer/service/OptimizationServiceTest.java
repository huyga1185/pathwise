package uk.huy.pathwise.routeoptimizer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationMultiVehicleRequest;
import uk.huy.pathwise.routeoptimizer.dto.request.OptimizationRequest;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationMultiVehicleResponse;
import uk.huy.pathwise.routeoptimizer.dto.response.OptimizationResponse;
import uk.huy.pathwise.routeoptimizer.routeoptimizationpipeline.RouteOptimizationPipeline;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptimizationServiceTest {

    @Mock
    private RouteOptimizationPipeline pipeline;

    @InjectMocks
    private OptimizationService service;

    private final List<String> addresses = List.of("a", "b", "c");

    @Test
    void optimizeRoute_delegatesToPipelineAndMapsResponse() {
        OptimizationRequest request = OptimizationRequest.builder()
                .addresses(addresses)
                .depot(1)
                .build();
        List<String> optimized = List.of("b", "a", "c");
        when(pipeline.optimize(addresses, 1)).thenReturn(optimized);

        OptimizationResponse response = service.optimizeRoute(request);

        assertThat(response.getOptimizedRoute()).isEqualTo(optimized);
        assertThat(response.getDepot()).isEqualTo(1);
        verify(pipeline).optimize(addresses, 1);
    }

    @Test
    void optimizeRoute_propagatesPipelineException() {
        OptimizationRequest request = OptimizationRequest.builder()
                .addresses(addresses)
                .depot(99)
                .build();
        when(pipeline.optimize(addresses, 99)).thenThrow(new AppException(ErrorCode.INVALID_DEPOT));

        assertThatThrownBy(() -> service.optimizeRoute(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPOT);
    }

    @Test
    void optimizeRouteForKVehicle_delegatesToPipelineAndMapsResponse() {
        OptimizationMultiVehicleRequest request =
                new OptimizationMultiVehicleRequest(addresses, 0, 2);
        List<List<String>> optimized = List.of(
                List.of("a", "b", "a"),
                List.of("a", "c", "a"));
        when(pipeline.optimizeKVehicleRoute(addresses, 0, 2)).thenReturn(optimized);

        OptimizationMultiVehicleResponse response = service.optimizeRouteForKVehicle(request);

        assertThat(response.optimizedRoute()).isEqualTo(optimized);
        assertThat(response.depot()).isEqualTo(0);
        verify(pipeline).optimizeKVehicleRoute(addresses, 0, 2);
    }

    @Test
    void optimizeRouteForKVehicle_propagatesPipelineException() {
        OptimizationMultiVehicleRequest request =
                new OptimizationMultiVehicleRequest(addresses, -1, 2);
        when(pipeline.optimizeKVehicleRoute(addresses, -1, 2))
                .thenThrow(new AppException(ErrorCode.INVALID_DEPOT));

        assertThatThrownBy(() -> service.optimizeRouteForKVehicle(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPOT);
    }

    @Test
    void optimizeRoute_passesThroughEmptyResult() {
        OptimizationRequest request = OptimizationRequest.builder()
                .addresses(addresses)
                .depot(0)
                .build();
        when(pipeline.optimize(anyList(), anyInt())).thenReturn(List.of());

        OptimizationResponse response = service.optimizeRoute(request);

        assertThat(response.getOptimizedRoute()).isEmpty();
        assertThat(response.getDepot()).isZero();
    }
}
