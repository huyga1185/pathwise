package uk.huy.pathwise.routing.infrastructure.ortools;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.LocalSearchMetaheuristic;
import com.google.ortools.constraintsolver.RoutingDimension;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.solver.SolverProvider;
import uk.huy.pathwise.routing.solver.VrpProblem;
import uk.huy.pathwise.routing.solver.VrpSolution;
import uk.huy.pathwise.routing.solver.VrpSolution.VrpRoute;
import uk.huy.pathwise.routing.solver.VrpSolution.VrpVisit;
import uk.huy.pathwise.routing.solver.VrpStop;
import uk.huy.pathwise.routing.solver.VrpVehicle;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * OR-Tools routing solver for {@link VrpProblem} (ARCHITECTURE.md §8.3):
 * <ul>
 *   <li>arc cost = travel duration;</li>
 *   <li>Capacity dimension on stop demand, bounded by each vehicle's capacity;</li>
 *   <li>Time dimension with transit = travel duration + service time at the origin, slack for waiting, stop cumuls
 *       inside the time window, route start and end inside the vehicle's shift;</li>
 *   <li>Distance dimension bounded by each vehicle's {@code maxDistanceMeters};</li>
 *   <li>every stop is optional with a large drop penalty, so stops that fit nowhere are dropped instead of making the
 *       whole problem infeasible (P-13);</li>
 *   <li>first solution {@code PATH_CHEAPEST_ARC}, then {@code GUIDED_LOCAL_SEARCH} until the time limit. GLS keeps
 *       improving until the limit, so every solve with stops takes the full time limit.</li>
 * </ul>
 * The drop penalty is far above any possible route cost (every vehicle driving the whole horizon), so the solver only
 * drops a stop when it cannot be served. Departure is pushed as late and arrival as early as the plan allows, so a
 * route does not start hours before its first stop needs it.
 */
public class OrToolsVrpSolver implements SolverProvider {
    static {
        Loader.loadNativeLibraries();
    }

    private static final long UNLIMITED_DISTANCE = Integer.MAX_VALUE;

    private final Duration timeLimit;

    public OrToolsVrpSolver(Duration timeLimit) {
        if (timeLimit == null || timeLimit.isNegative() || timeLimit.isZero())
            throw new IllegalArgumentException("Time limit must be positive");
        this.timeLimit = timeLimit;
    }

    @Override
    public VrpSolution solve(VrpProblem problem) {
        validate(problem);
        TravelMatrix matrix = problem.matrix();
        List<VrpVehicle> vehicles = problem.vehicles();
        List<VrpStop> stops = problem.stops();
        int vehicleCount = vehicles.size();

        int[] starts = vehicles.stream().mapToInt(VrpVehicle::startNode).toArray();
        int[] ends = vehicles.stream().mapToInt(VrpVehicle::endNode).toArray();
        RoutingIndexManager manager = new RoutingIndexManager(matrix.size(), vehicleCount, starts, ends);
        RoutingModel routing = new RoutingModel(manager);

        // Per node: demand, service time and the stop it belongs to (-1 for start/end nodes).
        int[] demand = new int[matrix.size()];
        int[] service = new int[matrix.size()];
        int[] stopOfNode = new int[matrix.size()];
        Arrays.fill(stopOfNode, -1);
        for (int s = 0; s < stops.size(); s++) {
            VrpStop stop = stops.get(s);
            demand[stop.node()] = stop.demand();
            service[stop.node()] = stop.serviceTimeSeconds();
            stopOfNode[stop.node()] = s;
        }
        int[][] durations = matrix.durationSeconds();
        int[][] distances = matrix.distanceMeters();

        int durationCallback = routing.registerTransitCallback((from, to) ->
                durations[manager.indexToNode(from)][manager.indexToNode(to)]);
        routing.setArcCostEvaluatorOfAllVehicles(durationCallback);

        int demandCallback = routing.registerUnaryTransitCallback(index -> demand[manager.indexToNode(index)]);
        routing.addDimensionWithVehicleCapacity(demandCallback, 0,
                vehicles.stream().mapToLong(VrpVehicle::capacity).toArray(), true, "Capacity");

        // Time windows may lie outside every shift; they must still fit the dimension, or OR-Tools fails while
        // building the model instead of simply dropping those stops.
        int horizon = Math.max(vehicles.stream().mapToInt(VrpVehicle::shiftEndSeconds).max().orElse(0),
                stops.stream().map(VrpStop::timeWindowEndSeconds).filter(Objects::nonNull)
                        .mapToInt(Integer::intValue).max().orElse(0));
        int timeCallback = routing.registerTransitCallback((from, to) -> {
            int fromNode = manager.indexToNode(from);
            return (long) durations[fromNode][manager.indexToNode(to)] + service[fromNode];
        });
        // Slack up to the horizon lets a vehicle wait for a time window to open; fix_start_cumul_to_zero = false.
        routing.addDimension(timeCallback, horizon, horizon, false, "Time");
        RoutingDimension time = routing.getMutableDimension("Time");

        int distanceCallback = routing.registerTransitCallback((from, to) ->
                distances[manager.indexToNode(from)][manager.indexToNode(to)]);
        routing.addDimensionWithVehicleCapacity(distanceCallback, 0,
                vehicles.stream().mapToLong(v -> v.maxDistanceMeters() == null ? UNLIMITED_DISTANCE : v.maxDistanceMeters()).toArray(),
                true, "Distance");
        RoutingDimension distance = routing.getMutableDimension("Distance");

        for (VrpStop stop : stops) {
            long index = manager.nodeToIndex(stop.node());
            if (stop.timeWindowStartSeconds() != null)
                time.cumulVar(index).setRange(stop.timeWindowStartSeconds(), stop.timeWindowEndSeconds());
            routing.addDisjunction(new long[]{index}, dropPenalty(vehicleCount, horizon));
        }
        for (int v = 0; v < vehicleCount; v++) {
            VrpVehicle vehicle = vehicles.get(v);
            time.cumulVar(routing.start(v)).setRange(vehicle.shiftStartSeconds(), vehicle.shiftEndSeconds());
            time.cumulVar(routing.end(v)).setRange(vehicle.shiftStartSeconds(), vehicle.shiftEndSeconds());
            routing.addVariableMaximizedByFinalizer(time.cumulVar(routing.start(v)));
            routing.addVariableMinimizedByFinalizer(time.cumulVar(routing.end(v)));
        }

        RoutingSearchParameters parameters = main.defaultRoutingSearchParameters().toBuilder()
                .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                .setLocalSearchMetaheuristic(LocalSearchMetaheuristic.Value.GUIDED_LOCAL_SEARCH)
                .setTimeLimit(com.google.protobuf.Duration.newBuilder()
                        .setSeconds(timeLimit.toSeconds())
                        .setNanos(timeLimit.toNanosPart()))
                .build();
        Assignment assignment = routing.solveWithParameters(parameters);
        if (assignment == null) return noSolution(stops.size());

        List<VrpRoute> routes = new ArrayList<>();
        Set<Integer> served = new HashSet<>();
        for (int v = 0; v < vehicleCount; v++) {
            List<VrpVisit> visits = new ArrayList<>();
            int load = 0;
            long index = assignment.value(routing.nextVar(routing.start(v)));
            while (!routing.isEnd(index)) {
                int stopIndex = stopOfNode[manager.indexToNode(index)];
                visits.add(new VrpVisit(stopIndex, (int) assignment.min(time.cumulVar(index))));
                load += stops.get(stopIndex).demand();
                served.add(stopIndex);
                index = assignment.value(routing.nextVar(index));
            }
            int start = (int) assignment.min(time.cumulVar(routing.start(v)));
            int end = (int) assignment.min(time.cumulVar(routing.end(v)));
            routes.add(new VrpRoute(v,
                    manager.indexToNode(routing.start(v)),
                    manager.indexToNode(routing.end(v)),
                    List.copyOf(visits),
                    start,
                    end,
                    (int) assignment.min(distance.cumulVar(routing.end(v))),
                    end - start,
                    load));
        }
        List<Integer> dropped = new ArrayList<>();
        for (int s = 0; s < stops.size(); s++) if (!served.contains(s)) dropped.add(s);
        return new VrpSolution(VrpSolution.Status.SOLVED, List.copyOf(routes), List.copyOf(dropped));
    }

    /**
     * More than the cost of every vehicle driving for the whole horizon, so dropping a stop never pays off when it can
     * be served.
     */
    private static long dropPenalty(int vehicleCount, int horizon) {
        return (long) (vehicleCount + 1) * Math.max(horizon, 1) * 100L;
    }

    private static VrpSolution noSolution(int stopCount) {
        List<Integer> dropped = new ArrayList<>();
        for (int s = 0; s < stopCount; s++) dropped.add(s);
        return new VrpSolution(VrpSolution.Status.NO_SOLUTION, List.of(), List.copyOf(dropped));
    }

    private static void validate(VrpProblem problem) {
        if (problem == null || problem.matrix() == null || problem.vehicles() == null || problem.stops() == null)
            throw new IllegalArgumentException("Matrix, vehicles and stops are required");
        if (problem.vehicles().isEmpty()) throw new IllegalArgumentException("At least one vehicle is required");
        int n = problem.matrix().size();
        if (problem.matrix().durationSeconds().length != n) throw new IllegalArgumentException("Matrices differ in size");
        boolean[] used = new boolean[n];
        Set<Integer> vehicleNodes = new HashSet<>();
        for (VrpVehicle vehicle : problem.vehicles()) {
            checkNode(vehicle.startNode(), n);
            checkNode(vehicle.endNode(), n);
            if (vehicle.capacity() < 0) throw new IllegalArgumentException("Capacity must not be negative");
            if (vehicle.shiftStartSeconds() < 0 || vehicle.shiftStartSeconds() >= vehicle.shiftEndSeconds())
                throw new IllegalArgumentException("Shift start must be before shift end");
            if (vehicle.maxDistanceMeters() != null && vehicle.maxDistanceMeters() <= 0)
                throw new IllegalArgumentException("Max distance must be positive");
            vehicleNodes.add(vehicle.startNode());
            vehicleNodes.add(vehicle.endNode());
            used[vehicle.startNode()] = true;
            used[vehicle.endNode()] = true;
        }
        for (VrpStop stop : problem.stops()) {
            checkNode(stop.node(), n);
            if (vehicleNodes.contains(stop.node()) || used[stop.node()])
                throw new IllegalArgumentException("Node " + stop.node() + " is used twice");
            used[stop.node()] = true;
            if (stop.demand() < 0 || stop.serviceTimeSeconds() < 0)
                throw new IllegalArgumentException("Demand and service time must not be negative");
            if ((stop.timeWindowStartSeconds() == null) != (stop.timeWindowEndSeconds() == null))
                throw new IllegalArgumentException("A time window needs both start and end");
            if (stop.timeWindowStartSeconds() != null && stop.timeWindowStartSeconds() > stop.timeWindowEndSeconds())
                throw new IllegalArgumentException("Time window start must not be after its end");
        }
        for (int node = 0; node < n; node++)
            if (!used[node]) throw new IllegalArgumentException("Node " + node + " is neither a stop nor a vehicle start or end");
    }

    private static void checkNode(int node, int n) {
        if (node < 0 || node >= n) throw new IllegalArgumentException("Node " + node + " is outside the matrix");
    }
}
