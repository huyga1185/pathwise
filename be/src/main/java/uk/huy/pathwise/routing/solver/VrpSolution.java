package uk.huy.pathwise.routing.solver;

import java.util.List;

/**
 * Result of {@link SolverProvider#solve}.
 *
 * @param status       {@code NO_SOLUTION} when the solver found none at all within the time limit (for example a
 *                     vehicle cannot even drive from its start to its end within its shift); routes are then empty
 *                     and every stop is dropped
 * @param routes       one route per vehicle, in the order of {@link VrpProblem#vehicles()}; an unused vehicle has no
 *                     visits
 * @param droppedStops indexes into {@link VrpProblem#stops()} of the stops no route could take (P-13)
 */
public record VrpSolution(Status status, List<VrpRoute> routes, List<Integer> droppedStops) {
    public enum Status { SOLVED, NO_SOLUTION }

    /**
     * @param visits          in driving order
     * @param startSeconds    departure from the start node
     * @param endSeconds      arrival at the end node
     * @param distanceMeters  start to end, along the visits
     * @param durationSeconds {@code endSeconds - startSeconds}: driving, service and waiting
     * @param load            sum of the demand of the visits in the vehicle's capacity unit
     */
    public record VrpRoute(int vehicleIndex,
                           int startNode,
                           int endNode,
                           List<VrpVisit> visits,
                           int startSeconds,
                           int endSeconds,
                           int distanceMeters,
                           int durationSeconds,
                           int load) {}

    /**
     * @param stopIndex      index into {@link VrpProblem#stops()}
     * @param arrivalSeconds planned start of service, within the stop's time window
     */
    public record VrpVisit(int stopIndex, int arrivalSeconds) {}
}
