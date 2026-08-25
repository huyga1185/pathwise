package uk.huy.pathwise.routeoptimizer.infrastructure.solver.ortoolsprovider;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.routeoptimizer.infrastructure.solver.SolverProvider;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;

import java.util.ArrayList;
import java.util.List;

@Component
@Qualifier("ORToolSolver")
public class ORToolsSolver implements SolverProvider {
    static {
        Loader.loadNativeLibraries();
    }

    private long[][] convertFrom2DArrayDoubleToLong(double[][] distances) {
        long[][] lDistances = new long[distances.length][distances.length];
        for (int i = 0; i < distances.length; i++) {
            for (int j = 0; j < distances.length; j++) {
                lDistances[i][j] = (long) distances[i][j];
            }
        }
        return lDistances;
    }

    private int createDistancesCallback(long[][] distances, RoutingModel routing, RoutingIndexManager manager) {
        return routing.registerTransitCallback((long fromIndex, long toIndex) -> {
            int fromNode = manager.indexToNode(fromIndex);
            int toNode = manager.indexToNode(toIndex);
            return distances[fromNode][toNode];
        });
    }

    @Override
    public int[] solveTSP(double[][] distances, int depot) {
        long[][] lDistances = convertFrom2DArrayDoubleToLong(distances);
        RoutingIndexManager manager = new RoutingIndexManager(lDistances.length, 1, depot);
        RoutingModel routing = new RoutingModel(manager);
        final int transitCallbackIndex = createDistancesCallback(lDistances, routing, manager);
        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);
        RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .build();
        Assignment solution = routing.solveWithParameters(searchParameters);
        if (solution == null) throw new AppException(ErrorCode.COULD_NOT_FIND_CHEAPEST_ROUTE);
        List<Integer> lResult = new ArrayList<>();
        long index = routing.start(0);
        while (!routing.isEnd(index)) {
            lResult.add(manager.indexToNode(index));
            index = solution.value(routing.nextVar(index));
        }
        lResult.add(manager.indexToNode(index));
        int[] result = new int[lResult.size()];
        for (int i = 0; i < lResult.size(); i++) {
            result[i] = lResult.get(i);
        }
        return result;
    }

    @Override
    public List<List<Integer>> solveVRP(double[][] distances, int depot, int vehicleNumber) {
        long[][] lDistances = convertFrom2DArrayDoubleToLong(distances);
        RoutingIndexManager manager = new RoutingIndexManager(lDistances.length, vehicleNumber, depot);
        RoutingModel routing = new RoutingModel(manager);
        final int transitCallbackIndex = createDistancesCallback(lDistances, routing, manager);
        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);
        boolean unused = routing.addDimension(
                transitCallbackIndex,
                0,
                50000000,
                true,
                "Distance");
        RoutingDimension distanceDimension = routing.getMutableDimension("Distance");
        distanceDimension.setGlobalSpanCostCoefficient(100);
        RoutingSearchParameters searchParameters = main.defaultRoutingSearchParameters()
                .toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .build();
        Assignment solution = routing.solveWithParameters(searchParameters);
        if (solution == null) throw new AppException(ErrorCode.COULD_NOT_FIND_CHEAPEST_ROUTE);
        List<List<Integer>> finalResult = new ArrayList<>();
        for (int i = 0; i < vehicleNumber; i++) {
            if (!routing.isVehicleUsed(solution, i)) continue;
            long index = routing.start(i);
            List<Integer> temp = new ArrayList<>();
            while (!routing.isEnd(index)) {
                temp.add(manager.indexToNode(index));
                index = solution.value(routing.nextVar(index));
            }
            temp.add(manager.indexToNode(index));
            finalResult.add(temp);
        }
        return finalResult;
    }
}
