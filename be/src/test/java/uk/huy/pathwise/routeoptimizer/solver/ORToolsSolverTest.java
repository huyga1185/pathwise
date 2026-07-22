package uk.huy.pathwise.routeoptimizer.solver;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.huy.pathwise.routeoptimizer.solver.ortools.ORToolsSolver;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real OR-Tools native solver (no mocking). The configured search only sets a
 * first-solution strategy, so we assert structural correctness of the route rather than a specific
 * optimal tour, which keeps the test stable across OR-Tools versions.
 */
class ORToolsSolverTest {

    private ORToolsSolver solver;

    // Classic symmetric 4-node distance matrix.
    private double[][] matrix;

    @BeforeEach
    void setUp() {
        solver = new ORToolsSolver();
        matrix = new double[][] {
                {  0, 10, 15, 20 },
                { 10,  0, 35, 25 },
                { 15, 35,  0, 30 },
                { 20, 25, 30,  0 },
        };
    }

    @Test
    void solveTSP_returnsClosedTourVisitingEveryNodeOnce() {
        int[] route = solver.solveTSP(matrix, 0);

        // A closed tour over n nodes has n+1 stops (returns to the depot).
        assertThat(route).hasSize(matrix.length + 1);
        assertThat(route[0]).isEqualTo(0);
        assertThat(route[route.length - 1]).isEqualTo(0);

        // Dropping the closing depot, every node 0..n-1 must appear exactly once.
        int[] withoutReturn = java.util.Arrays.copyOf(route, route.length - 1);
        assertThat(withoutReturn).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    void solveTSP_startsAndEndsAtGivenDepot() {
        int depot = 2;

        int[] route = solver.solveTSP(matrix, depot);

        assertThat(route[0]).isEqualTo(depot);
        assertThat(route[route.length - 1]).isEqualTo(depot);
        int[] withoutReturn = java.util.Arrays.copyOf(route, route.length - 1);
        assertThat(withoutReturn).containsExactlyInAnyOrder(0, 1, 2, 3);
    }

    @Test
    void solveTSP_singleNode_returnsDepotOnly() {
        int[] route = solver.solveTSP(new double[][]{{0}}, 0);

        // start node + closing node, both the depot.
        assertThat(route).containsExactly(0, 0);
    }

    @Test
    void solveVRP_everyNonDepotNodeServedExactlyOnceAcrossVehicles() {
        int depot = 0;
        int vehicles = 2;

        List<List<Integer>> routes = solver.solveVRP(matrix, depot, vehicles);

        assertThat(routes).isNotEmpty();
        assertThat(routes.size()).isLessThanOrEqualTo(vehicles);

        // Each returned route is a closed loop that starts and ends at the depot.
        for (List<Integer> route : routes) {
            assertThat(route).isNotEmpty();
            assertThat(route.get(0)).isEqualTo(depot);
            assertThat(route.get(route.size() - 1)).isEqualTo(depot);
        }

        // Across all routes, the customer nodes 1,2,3 are each visited exactly once.
        List<Integer> customers = routes.stream()
                .flatMap(List::stream)
                .filter(node -> node != depot)
                .toList();
        assertThat(customers).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void solveVRP_singleVehicle_behavesLikeOneClosedTour() {
        List<List<Integer>> routes = solver.solveVRP(matrix, 0, 1);

        assertThat(routes).hasSize(1);
        List<Integer> route = routes.get(0);
        assertThat(route.get(0)).isEqualTo(0);
        assertThat(route.get(route.size() - 1)).isEqualTo(0);
        assertThat(route).containsExactlyInAnyOrder(0, 1, 2, 3, 0);
    }

    @Test
    void solveVRP_respectsGivenDepot() {
        int depot = 3;

        List<List<Integer>> routes = solver.solveVRP(matrix, depot, 2);

        for (List<Integer> route : routes) {
            assertThat(route.get(0)).isEqualTo(depot);
            assertThat(route.get(route.size() - 1)).isEqualTo(depot);
        }
    }
}
