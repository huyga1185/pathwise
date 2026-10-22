package uk.huy.pathwise.routeoptimizer.infrastructure.solver;

import java.util.List;

public interface SolverProvider {
    int[] solveTSP(double[][] distances, int depot);
    List<List<Integer>> solveVRP(double[][] distances, int depot, int vehicleNumber);
}
