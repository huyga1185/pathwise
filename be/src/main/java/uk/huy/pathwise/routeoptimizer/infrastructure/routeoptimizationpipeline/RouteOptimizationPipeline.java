package uk.huy.pathwise.routeoptimizer.infrastructure.routeoptimizationpipeline;

import lombok.AllArgsConstructor;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingResult;
import uk.huy.pathwise.routeoptimizer.infrastructure.matrix.MatrixProvider;
import uk.huy.pathwise.routeoptimizer.model.Coordinate;
import uk.huy.pathwise.routeoptimizer.infrastructure.solver.SolverProvider;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class RouteOptimizationPipeline {
    private final GeocodingProvider geocodingProvider;
    private final MatrixProvider matrixProvider;
    private final SolverProvider solverProvider;

    public List<String> optimize(List<String> addresses, int depot) {
        if (depot >= addresses.size() || depot < 0) throw new AppException(ErrorCode.INVALID_DEPOT);
        List<Coordinate> coordinates = geocode(addresses);
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
        List<Coordinate> coordinates = geocode(addresses);
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

    /**
     * One search per address; the shared rate limit spaces the calls (T21 removed the 1 s sleep per address).
     */
    private List<Coordinate> geocode(List<String> addresses) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (String address : addresses) {
            List<GeocodingResult> results = geocodingProvider.search(address, 1);
            if (results.isEmpty()) throw new AppException(ErrorCode.COULD_NOT_FIND_ADDRESS);
            coordinates.add(new Coordinate(results.getFirst().lat(), results.getFirst().lon()));
        }
        return coordinates;
    }
}
