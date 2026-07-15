package uk.huy.pathwise.routeoptimizer.routeoptimizationpipeline;

import lombok.AllArgsConstructor;
import uk.huy.pathwise.routeoptimizer.geocoding.GeocodingProvider;
import uk.huy.pathwise.routeoptimizer.matrix.MatrixProvider;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;
import uk.huy.pathwise.routeoptimizer.solver.SolverProvider;
import uk.huy.pathwise.shared.exception.AppException;
import uk.huy.pathwise.shared.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class RouteOptimizationPipeline {
    private final GeocodingProvider geocodingProvider;
    private final MatrixProvider matrixProvider;
    private final SolverProvider solverProvider;

    public List<String> optimize(List<String> addresses, int depot) {
        if (depot >= addresses.size() || depot < 0) throw new AppException(ErrorCode.INVALID_DEPOT);
        List<Coordinate> coordinates = geocodingProvider.geocodeListAddress(addresses);
        double[][] matrix = matrixProvider.matrixRoute(coordinates, "driving");
        int[] result = solverProvider.solveTSP(matrix, depot);
        List<String> finalResult = new ArrayList<>();
        for (int r : result) {
            finalResult.add(addresses.get(r));
        }
        return finalResult;
    }

    public List<List<String>> optimizeKVehicleRoute(List<String> addresses, int depot, int numberVehicle) {
        if (depot >= addresses.size() || depot < 0) throw new AppException(ErrorCode.INVALID_DEPOT);
        List<Coordinate> coordinates = geocodingProvider.geocodeListAddress(addresses);
        double[][] matrix = matrixProvider.matrixRoute(coordinates, "driving");
        List<List<Integer>> result = solverProvider.solveVRP(matrix, depot, numberVehicle);
        List<List<String>> finalResult = new ArrayList<>();
        for (List<Integer> integers : result) {
            List<String> temp = new ArrayList<>();
            for (Integer integer : integers) {
                temp.add(addresses.get(integer));
            }
            finalResult.add(temp);
        }
        return finalResult;
    }
}
