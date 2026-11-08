package uk.huy.pathwise.routing.solver;

/**
 * Public API of the {@code routing} module for route optimization (ARCHITECTURE.md §8.3, §8.5). Pure computation:
 * it calls no external service and runs for at most the configured time limit.
 */
public interface SolverProvider {
    /**
     * @throws IllegalArgumentException if the problem is malformed (node out of range, a node used by two stops or by
     *                                  a stop and a vehicle, window or shift with start after end, negative values)
     */
    VrpSolution solve(VrpProblem problem);
}
