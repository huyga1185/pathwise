package uk.huy.pathwise.routeoptimizer.routeoptimizationpipeline;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.routeoptimizer.geocoding.GeocodingProvider;
import uk.huy.pathwise.routeoptimizer.matrix.MatrixProvider;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;
import uk.huy.pathwise.routeoptimizer.solver.SolverProvider;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class RouteOptimizationPipelineTest {

    @Mock
    private GeocodingProvider geocodingProvider;

    @Mock
    private MatrixProvider matrixProvider;

    @Mock
    private SolverProvider solverProvider;

    @InjectMocks
    private RouteOptimizationPipeline routeOptimizationPipeline;

    private Coordinate add1; // 14 doan uan
    private Coordinate add2; // gan cau tranthily
    private Coordinate add3; // gan cau rong
    private Coordinate add4; // gan cau song han
    private double[][] matrix;
    private int[] tspResult;
    private List<List<Integer>> vrpResult;
    private List<String> addresses;

    @BeforeEach
    void setUp() {
        add1 = new Coordinate(16.034, 108.242);
        add2 = new Coordinate(16.044, 108.237);
        add3 = new Coordinate(16.049, 108.220);
        add4 = new Coordinate(16.060, 108.222);

        addresses = new ArrayList<>();
        addresses.add("14 doan uan");
        addresses.add("gan cau tran thi ly");
        addresses.add("gan cau rong");
        addresses.add("gan cau song han");

        matrix = new double[][] {
            {    0.0, 1230.0, 2640.0, 3460.0 },
            { 1230.0,    0.0, 1870.0, 2330.0 },
            { 2640.0, 1870.0,    0.0, 1240.0 },
            { 3460.0, 2330.0, 1240.0,    0.0 },
        };

        tspResult = new int[] {0, 1, 2, 3};
        vrpResult = List.of(
                List.of(0, 1, 2, 0),
                List.of(0, 3, 0)
        );
    }

    @Test
    void optimizeTSP_allStopsGeocoded_returnsListString() {
        when(geocodingProvider.geocodeListAddress(anyList())).thenReturn(List.of(new Coordinate[]{add1, add2, add3, add4}));
        when(matrixProvider.matrixRoute(anyList(), anyString())).thenReturn(matrix);
        when(solverProvider.solveTSP(any(), anyInt())).thenReturn(tspResult);

        var rs = routeOptimizationPipeline.optimize(addresses, 0);

        assertThat(rs).isNotNull();
        assertThat(rs).isNotEmpty();

        var inOrder = inOrder(geocodingProvider, matrixProvider, solverProvider);
        inOrder.verify(geocodingProvider, atLeastOnce()).geocodeListAddress(anyList());
        inOrder.verify(matrixProvider).matrixRoute(anyList(), anyString());
        inOrder.verify(solverProvider).solveTSP(any(), anyInt());
    }

    @Test
    void optimizeTSP_mapsSolverIndicesBackToAddressesInOrder() {
        // solver visits stops in the order 3 -> 0 -> 2 -> 1
        when(geocodingProvider.geocodeListAddress(anyList())).thenReturn(List.of(new Coordinate[]{add1, add2, add3, add4}));
        when(matrixProvider.matrixRoute(anyList(), anyString())).thenReturn(matrix);
        when(solverProvider.solveTSP(any(), anyInt())).thenReturn(new int[]{3, 0, 2, 1});

        var rs = routeOptimizationPipeline.optimize(addresses, 0);

        assertThat(rs).containsExactly(
                "gan cau song han",
                "14 doan uan",
                "gan cau rong",
                "gan cau tran thi ly"
        );
    }

    @Test
    void optimizeTSP_passesGeocodedCoordinatesDrivingProfileAndDepotDownstream() {
        List<Coordinate> coordinates = List.of(add1, add2, add3, add4);
        when(geocodingProvider.geocodeListAddress(anyList())).thenReturn(coordinates);
        when(matrixProvider.matrixRoute(anyList(), anyString())).thenReturn(matrix);
        when(solverProvider.solveTSP(any(), anyInt())).thenReturn(tspResult);

        routeOptimizationPipeline.optimize(addresses, 2);

        verify(geocodingProvider).geocodeListAddress(addresses);
        verify(matrixProvider).matrixRoute(coordinates, "driving");
        verify(solverProvider).solveTSP(matrix, 2);
    }

    @Test
    void optimizeTSP_depotEqualToSize_throwsInvalidDepotAndSkipsProviders() {
        assertThatThrownBy(() -> routeOptimizationPipeline.optimize(addresses, addresses.size()))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPOT);

        verifyNoInteractions(geocodingProvider, matrixProvider, solverProvider);
    }

    @Test
    void optimizeTSP_depotGreaterThanSize_throwsInvalidDepot() {
        assertThatThrownBy(() -> routeOptimizationPipeline.optimize(addresses, 99))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPOT);

        verifyNoInteractions(geocodingProvider, matrixProvider, solverProvider);
    }

    @Test
    void optimizeTSP_negativeDepot_throwsInvalidDepot() {
        assertThatThrownBy(() -> routeOptimizationPipeline.optimize(addresses, -1))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPOT);

        verifyNoInteractions(geocodingProvider, matrixProvider, solverProvider);
    }

    @Test
    void optimizeVRP_allStopsGeocoded_returns2DArrayString() {
        when(geocodingProvider.geocodeListAddress(anyList())).thenReturn(List.of(new Coordinate[]{add1, add2, add3, add4}));
        when(matrixProvider.matrixRoute(anyList(), anyString())).thenReturn(matrix);
        when(solverProvider.solveVRP(any(), anyInt(), anyInt())).thenReturn(vrpResult);

        var rs = routeOptimizationPipeline.optimizeKVehicleRoute(addresses, 0, 2);

        assertThat(rs).isNotNull();
        assertThat(rs).isNotEmpty();

        var inOrder = inOrder(geocodingProvider, matrixProvider, solverProvider);
        inOrder.verify(geocodingProvider, atLeastOnce()).geocodeListAddress(anyList());
        inOrder.verify(matrixProvider).matrixRoute(anyList(), anyString());
        inOrder.verify(solverProvider).solveVRP(any(), anyInt(), anyInt());
    }

    @Test
    void optimizeVRP_mapsEachVehicleRouteBackToAddresses() {
        when(geocodingProvider.geocodeListAddress(anyList())).thenReturn(List.of(new Coordinate[]{add1, add2, add3, add4}));
        when(matrixProvider.matrixRoute(anyList(), anyString())).thenReturn(matrix);
        when(solverProvider.solveVRP(any(), anyInt(), anyInt())).thenReturn(vrpResult);

        var rs = routeOptimizationPipeline.optimizeKVehicleRoute(addresses, 0, 2);

        assertThat(rs).hasSize(2);
        assertThat(rs.get(0)).containsExactly(
                "14 doan uan",
                "gan cau tran thi ly",
                "gan cau rong",
                "14 doan uan"
        );
        assertThat(rs.get(1)).containsExactly(
                "14 doan uan",
                "gan cau song han",
                "14 doan uan"
        );
    }

    @Test
    void optimizeVRP_passesGeocodedCoordinatesDrivingProfileDepotAndVehicleCountDownstream() {
        List<Coordinate> coordinates = List.of(add1, add2, add3, add4);
        when(geocodingProvider.geocodeListAddress(anyList())).thenReturn(coordinates);
        when(matrixProvider.matrixRoute(anyList(), anyString())).thenReturn(matrix);
        when(solverProvider.solveVRP(any(), anyInt(), anyInt())).thenReturn(vrpResult);

        routeOptimizationPipeline.optimizeKVehicleRoute(addresses, 1, 3);

        ArgumentCaptor<String> profile = ArgumentCaptor.forClass(String.class);
        verify(geocodingProvider).geocodeListAddress(addresses);
        verify(matrixProvider).matrixRoute(eq(coordinates), profile.capture());
        verify(solverProvider).solveVRP(matrix, 1, 3);
        assertThat(profile.getValue()).isEqualTo("driving");
    }

    @Test
    void optimizeVRP_depotEqualToSize_throwsInvalidDepotAndSkipsProviders() {
        assertThatThrownBy(() -> routeOptimizationPipeline.optimizeKVehicleRoute(addresses, addresses.size(), 2))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPOT);

        verifyNoInteractions(geocodingProvider, matrixProvider, solverProvider);
    }

    @Test
    void optimizeVRP_negativeDepot_throwsInvalidDepot() {
        assertThatThrownBy(() -> routeOptimizationPipeline.optimizeKVehicleRoute(addresses, -1, 2))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_DEPOT);

        verifyNoInteractions(geocodingProvider, matrixProvider, solverProvider);
    }
}
