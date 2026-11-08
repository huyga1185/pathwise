package uk.huy.pathwise.routing;

import org.junit.jupiter.api.Test;
import uk.huy.pathwise.routing.infrastructure.ortools.OrToolsVrpSolver;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.solver.VrpProblem;
import uk.huy.pathwise.routing.solver.VrpSolution;
import uk.huy.pathwise.routing.solver.VrpSolution.VrpRoute;
import uk.huy.pathwise.routing.solver.VrpSolution.VrpVisit;
import uk.huy.pathwise.routing.solver.VrpStop;
import uk.huy.pathwise.routing.solver.VrpVehicle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * T23 acceptance with fixed matrices. Points lie on a plane in metres; travel at 10 m/s. Times are seconds from 00:00.
 */
class OrToolsVrpSolverTest {
    private static final int H8 = 8 * 3600;
    private static final int H12 = 12 * 3600;
    private static final int H17 = 17 * 3600;
    private static final String BOX = "BOX";
    private static final String KG = "KG";

    private final OrToolsVrpSolver solver = new OrToolsVrpSolver(Duration.ofSeconds(1));

    @Test
    void solve_whenDemandsFillTwoVehicles_keepEveryRouteWithinCapacity() {
        // Node 0 depot, 1..6 stops of demand 3; two vehicles of capacity 9 hold exactly 18.
        TravelMatrix matrix = planar(new int[][]{{0, 0}, {1000, 0}, {2000, 0}, {3000, 0}, {0, 1000}, {0, 2000}, {0, 3000}});
        List<VrpStop> stops = new ArrayList<>();
        for (int node = 1; node <= 6; node++) stops.add(stop(node, 3, null, null));
        List<VrpVehicle> vehicles = List.of(vehicle(0, 0, 9, H8, H17, null), vehicle(0, 0, 9, H8, H17, null));

        VrpSolution solution = solver.solve(new VrpProblem(matrix, vehicles, stops));

        assertThat(solution.status()).isEqualTo(VrpSolution.Status.SOLVED);
        assertThat(solution.droppedStops()).isEmpty();
        for (VrpRoute route : solution.routes()) {
            assertThat(route.load()).isLessThanOrEqualTo(9);
            assertThat(route.load()).isEqualTo(route.visits().stream().mapToInt(v -> stops.get(v.stopIndex()).demand(BOX)).sum());
        }
        assertConsistent(matrix, vehicles, stops, solution);
    }

    @Test
    void solve_whenStopsHaveTimeWindows_arriveInsideEveryWindow() {
        TravelMatrix matrix = planar(new int[][]{{0, 0}, {3000, 0}, {6000, 0}, {9000, 0}, {6000, 3000}});
        List<VrpStop> stops = List.of(
                stop(1, 1, 10 * 3600, 10 * 3600 + 1800),   // 10:00-10:30
                stop(2, 1, H8, 9 * 3600),                  // 08:00-09:00
                stop(3, 1, 11 * 3600, H12),                // 11:00-12:00
                stop(4, 1, 9 * 3600, 9 * 3600 + 900));     // 09:00-09:15
        List<VrpVehicle> vehicles = List.of(vehicle(0, 0, 10, H8, H17, null));

        VrpSolution solution = solver.solve(new VrpProblem(matrix, vehicles, stops));

        assertThat(solution.droppedStops()).isEmpty();
        for (VrpVisit visit : solution.routes().getFirst().visits()) {
            VrpStop stop = stops.get(visit.stopIndex());
            assertThat(visit.arrivalSeconds()).isBetween(stop.timeWindowStartSeconds(), stop.timeWindowEndSeconds());
        }
        assertConsistent(matrix, vehicles, stops, solution);
    }

    @Test
    void solve_whenShiftIsShort_startAndEndInsideShiftAndDropWhatDoesNotFit() {
        // Shift 08:00-12:00. Stop 2 only opens at 14:00, after the shift.
        TravelMatrix matrix = planar(new int[][]{{0, 0}, {6000, 0}, {0, 6000}, {-6000, 0}});
        List<VrpStop> stops = List.of(
                stop(1, 1, null, null),
                stop(2, 1, 14 * 3600, 15 * 3600),
                stop(3, 1, null, null));
        List<VrpVehicle> vehicles = List.of(vehicle(0, 0, 10, H8, H12, null));

        VrpSolution solution = solver.solve(new VrpProblem(matrix, vehicles, stops));

        VrpRoute route = solution.routes().getFirst();
        assertThat(route.startSeconds()).isGreaterThanOrEqualTo(H8);
        assertThat(route.endSeconds()).isLessThanOrEqualTo(H12);
        assertThat(solution.droppedStops()).containsExactly(1);
        assertThat(route.visits()).extracting(VrpVisit::stopIndex).containsExactlyInAnyOrder(0, 2);
        assertConsistent(matrix, vehicles, stops, solution);
    }

    @Test
    void solve_whenMaxDistanceSet_keepRouteWithinItAndDropTheFarStop() {
        // A round trip to stop 2 is 40 km; the limit is 25 km.
        TravelMatrix matrix = planar(new int[][]{{0, 0}, {5000, 0}, {20000, 0}, {0, 5000}});
        List<VrpStop> stops = List.of(stop(1, 1, null, null), stop(2, 1, null, null), stop(3, 1, null, null));
        List<VrpVehicle> vehicles = List.of(vehicle(0, 0, 10, H8, H17, 25_000));

        VrpSolution solution = solver.solve(new VrpProblem(matrix, vehicles, stops));

        assertThat(solution.routes().getFirst().distanceMeters()).isLessThanOrEqualTo(25_000);
        assertThat(solution.droppedStops()).containsExactly(1);
        assertConsistent(matrix, vehicles, stops, solution);
    }

    @Test
    void solve_whenVehiclesCountDifferentUnits_limitEachVehicleOnlyInItsOwnUnit() {
        // Truck counts 100 kg, motorbike 2 boxes. Stops 1-3: 40 kg each (120 kg in total, more than the truck),
        // stops 4-5: 1 box each.
        TravelMatrix matrix = planar(new int[][]{{0, 0}, {1000, 0}, {2000, 0}, {3000, 0}, {0, 1000}, {0, 2000}});
        List<VrpStop> stops = List.of(
                new VrpStop(1, Map.of(KG, 40), 300, null, null),
                new VrpStop(2, Map.of(KG, 40), 300, null, null),
                new VrpStop(3, Map.of(KG, 40), 300, null, null),
                new VrpStop(4, Map.of(BOX, 1), 300, null, null),
                new VrpStop(5, Map.of(BOX, 1), 300, null, null));
        List<VrpVehicle> vehicles = List.of(
                new VrpVehicle(0, 0, KG, 100, H8, H17, null),
                new VrpVehicle(0, 0, BOX, 2, H8, H17, null));

        VrpSolution solution = solver.solve(new VrpProblem(matrix, vehicles, stops));

        // Every stop fits: the motorbike is not limited in kg, so it takes the kg stop the truck cannot.
        assertThat(solution.droppedStops()).isEmpty();
        VrpRoute truck = solution.routes().get(0);
        VrpRoute motorbike = solution.routes().get(1);
        assertThat(truck.load()).isLessThanOrEqualTo(100);
        assertThat(motorbike.load()).isLessThanOrEqualTo(2);
        assertConsistent(matrix, vehicles, stops, solution);
    }

    @Test
    void solve_whenStopDemandExceedsEveryCapacity_dropItAndServeTheRest() {
        TravelMatrix matrix = planar(new int[][]{{0, 0}, {1000, 0}, {2000, 0}, {3000, 0}});
        List<VrpStop> stops = List.of(stop(1, 2, null, null), stop(2, 100, null, null), stop(3, 2, null, null));
        List<VrpVehicle> vehicles = List.of(vehicle(0, 0, 10, H8, H17, null), vehicle(0, 0, 10, H8, H17, null));

        VrpSolution solution = solver.solve(new VrpProblem(matrix, vehicles, stops));

        assertThat(solution.status()).isEqualTo(VrpSolution.Status.SOLVED);
        assertThat(solution.droppedStops()).containsExactly(1);
        assertThat(solution.routes()).flatExtracting(VrpRoute::visits).extracting(VrpVisit::stopIndex)
                .containsExactlyInAnyOrder(0, 2);
    }

    @Test
    void solve_whenVehiclesHaveTheirOwnStartAndEnd_routeStartsAndEndsAtThoseNodes() {
        // Vehicle 0: node 0 → node 1. Vehicle 1: node 2 → node 2. Stops 3..6.
        TravelMatrix matrix = planar(new int[][]{{0, 0}, {10000, 0}, {0, 10000}, {3000, 0}, {7000, 0}, {0, 4000}, {0, 7000}});
        List<VrpStop> stops = List.of(stop(3, 1, null, null), stop(4, 1, null, null), stop(5, 1, null, null), stop(6, 1, null, null));
        List<VrpVehicle> vehicles = List.of(vehicle(0, 1, 10, H8, H17, null), vehicle(2, 2, 10, H8, H17, null));

        VrpSolution solution = solver.solve(new VrpProblem(matrix, vehicles, stops));

        assertThat(solution.routes()).extracting(VrpRoute::vehicleIndex, VrpRoute::startNode, VrpRoute::endNode)
                .containsExactly(tuple(0, 0, 1), tuple(1, 2, 2));
        assertThat(solution.droppedStops()).isEmpty();
        assertConsistent(matrix, vehicles, stops, solution);
    }

    @Test
    void solve_when100StopsAnd20Vehicles_finishWithinTimeLimitPlusOneSecond() {
        Random random = new Random(42);
        int[][] points = new int[101][];
        points[0] = new int[]{0, 0};
        for (int i = 1; i <= 100; i++) points[i] = new int[]{random.nextInt(40_000) - 20_000, random.nextInt(40_000) - 20_000};
        TravelMatrix matrix = planar(points);
        List<VrpStop> stops = new ArrayList<>();
        for (int node = 1; node <= 100; node++) {
            int open = H8 + random.nextInt(6) * 3600;
            stops.add(random.nextBoolean() ? stop(node, 1 + random.nextInt(5), open, open + 2 * 3600) : stop(node, 1 + random.nextInt(5), null, null));
        }
        List<VrpVehicle> vehicles = new ArrayList<>();
        for (int v = 0; v < 20; v++) vehicles.add(vehicle(0, 0, 30, H8, H17, 150_000));
        OrToolsVrpSolver twoSeconds = new OrToolsVrpSolver(Duration.ofSeconds(2));

        long start = System.nanoTime();
        VrpSolution solution = twoSeconds.solve(new VrpProblem(matrix, vehicles, stops));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThanOrEqualTo(Duration.ofSeconds(3));
        assertThat(solution.status()).isEqualTo(VrpSolution.Status.SOLVED);
        assertConsistent(matrix, vehicles, stops, solution);
    }

    @Test
    void solve_whenNoStops_returnEmptyRoutes() {
        TravelMatrix matrix = planar(new int[][]{{0, 0}});

        VrpSolution solution = solver.solve(new VrpProblem(matrix, List.of(vehicle(0, 0, 10, H8, H17, null)), List.of()));

        assertThat(solution.status()).isEqualTo(VrpSolution.Status.SOLVED);
        assertThat(solution.routes()).singleElement().satisfies(route -> assertThat(route.visits()).isEmpty());
        assertThat(solution.droppedStops()).isEmpty();
    }

    @Test
    void solve_whenStopSharesAVehicleNodeOrANodeIsUnused_throwIllegalArgument() {
        TravelMatrix matrix = planar(new int[][]{{0, 0}, {1000, 0}, {2000, 0}});
        List<VrpVehicle> vehicles = List.of(vehicle(0, 0, 10, H8, H17, null));

        assertThatThrownBy(() -> solver.solve(new VrpProblem(matrix, vehicles, List.of(stop(0, 1, null, null), stop(1, 1, null, null), stop(2, 1, null, null)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> solver.solve(new VrpProblem(matrix, vehicles, List.of(stop(1, 1, null, null)))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> solver.solve(new VrpProblem(matrix, vehicles, List.of(stop(1, 1, 9 * 3600, H8), stop(2, 1, null, null)))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Checks a solution against the problem: every stop served once or dropped, timings follow travel and service
     * times, and every route respects capacity, shift, time windows and max distance.
     */
    private static void assertConsistent(TravelMatrix matrix, List<VrpVehicle> vehicles, List<VrpStop> stops, VrpSolution solution) {
        List<Integer> seen = new ArrayList<>(solution.droppedStops());
        for (VrpRoute route : solution.routes()) {
            VrpVehicle vehicle = vehicles.get(route.vehicleIndex());
            assertThat(route.startNode()).isEqualTo(vehicle.startNode());
            assertThat(route.endNode()).isEqualTo(vehicle.endNode());
            assertThat(route.startSeconds()).isGreaterThanOrEqualTo(vehicle.shiftStartSeconds());
            assertThat(route.endSeconds()).isLessThanOrEqualTo(vehicle.shiftEndSeconds());
            assertThat(route.load()).isLessThanOrEqualTo(vehicle.capacity());
            assertThat(route.load()).isEqualTo(route.visits().stream()
                    .mapToInt(visit -> stops.get(visit.stopIndex()).demand(vehicle.capacityUnit())).sum());
            int node = vehicle.startNode();
            int clock = route.startSeconds();
            int service = 0;
            int distance = 0;
            for (VrpVisit visit : route.visits()) {
                VrpStop stop = stops.get(visit.stopIndex());
                seen.add(visit.stopIndex());
                int earliest = clock + service + matrix.durationSeconds()[node][stop.node()];
                assertThat(visit.arrivalSeconds()).isGreaterThanOrEqualTo(earliest);
                if (stop.timeWindowStartSeconds() != null)
                    assertThat(visit.arrivalSeconds()).isBetween(stop.timeWindowStartSeconds(), stop.timeWindowEndSeconds());
                distance += matrix.distanceMeters()[node][stop.node()];
                node = stop.node();
                clock = visit.arrivalSeconds();
                service = stop.serviceTimeSeconds();
            }
            distance += matrix.distanceMeters()[node][vehicle.endNode()];
            assertThat(route.endSeconds()).isGreaterThanOrEqualTo(clock + service + matrix.durationSeconds()[node][vehicle.endNode()]);
            assertThat(route.distanceMeters()).isEqualTo(distance);
            if (vehicle.maxDistanceMeters() != null) assertThat(distance).isLessThanOrEqualTo(vehicle.maxDistanceMeters());
            assertThat(route.durationSeconds()).isEqualTo(route.endSeconds() - route.startSeconds());
        }
        assertThat(seen).containsExactlyInAnyOrderElementsOf(IntStream.range(0, stops.size()).boxed().toList());
    }

    private static TravelMatrix planar(int[][] points) {
        int n = points.length;
        int[][] distances = new int[n][n];
        int[][] durations = new int[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                distances[i][j] = (int) Math.round(Math.hypot(points[i][0] - points[j][0], points[i][1] - points[j][1]));
                durations[i][j] = Math.round(distances[i][j] / 10f);
            }
        return new TravelMatrix(distances, durations);
    }

    private static VrpStop stop(int node, int boxes, Integer windowStart, Integer windowEnd) {
        return new VrpStop(node, Map.of(BOX, boxes), 300, windowStart, windowEnd);
    }

    private static VrpVehicle vehicle(int start, int end, int boxes, int shiftStart, int shiftEnd, Integer maxDistance) {
        return new VrpVehicle(start, end, BOX, boxes, shiftStart, shiftEnd, maxDistance);
    }
}
