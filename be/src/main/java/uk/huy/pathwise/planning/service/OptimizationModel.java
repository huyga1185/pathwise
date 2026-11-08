package uk.huy.pathwise.planning.service;

import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.model.Coordinate;
import uk.huy.pathwise.routing.solver.VrpProblem;
import uk.huy.pathwise.routing.solver.VrpStop;
import uk.huy.pathwise.routing.solver.VrpVehicle;

import java.util.*;

/**
 * Turns an {@link OptimizationInput} into matrix nodes and a {@link VrpProblem} (ARCHITECTURE.md §8.3). Route starts
 * and ends share a node when they are the same point (the plan depot in the MVP); every stop has its own node, even
 * at the same coordinates as another, as the solver requires. Times are seconds from 00:00 of the plan date in the
 * plan timezone (P-14).
 */
final class OptimizationModel {
    private final OptimizationInput input;
    private final List<Coordinate> nodes = new ArrayList<>();
    private final int[] startNodes;
    private final int[] endNodes;
    private final int firstStopNode;

    private OptimizationModel(OptimizationInput input) {
        this.input = input;
        Map<Coordinate, Integer> places = new LinkedHashMap<>();
        startNodes = new int[input.routes().size()];
        endNodes = new int[input.routes().size()];
        for (int i = 0; i < input.routes().size(); i++) {
            OptimizationInput.Route route = input.routes().get(i);
            startNodes[i] = places.computeIfAbsent(route.start(), point -> places.size());
            endNodes[i] = places.computeIfAbsent(route.end(), point -> places.size());
        }
        nodes.addAll(places.keySet());
        firstStopNode = nodes.size();
        input.stops().forEach(stop -> nodes.add(stop.location()));
    }

    static OptimizationModel of(OptimizationInput input) {
        return new OptimizationModel(input);
    }

    /**
     * @return the points to ask the matrix for, in node order
     */
    List<Coordinate> nodes() {
        return Collections.unmodifiableList(nodes);
    }

    VrpProblem toProblem(TravelMatrix matrix) {
        List<VrpVehicle> vehicles = new ArrayList<>();
        for (int i = 0; i < input.routes().size(); i++) {
            OptimizationInput.Route route = input.routes().get(i);
            vehicles.add(new VrpVehicle(startNodes[i],
                    endNodes[i],
                    route.capacityUnit().name(),
                    route.capacity(),
                    route.shiftStart().toSecondOfDay(),
                    route.shiftEnd().toSecondOfDay(),
                    route.maxDistanceMeters()));
        }
        // ORDER is never entered: every stop counts one order when some vehicle is limited in orders (D-36).
        boolean countsOrders = input.routes().stream().anyMatch(route -> route.capacityUnit() == CapacityUnit.ORDER);
        List<VrpStop> stops = new ArrayList<>();
        for (int i = 0; i < input.stops().size(); i++) {
            OptimizationInput.Stop stop = input.stops().get(i);
            Map<String, Integer> demands = new HashMap<>();
            stop.demands().forEach((unit, quantity) -> demands.put(unit.name(), quantity));
            if (countsOrders) demands.put(CapacityUnit.ORDER.name(), 1);
            stops.add(new VrpStop(firstStopNode + i,
                    demands,
                    stop.serviceTimeSeconds(),
                    stop.timeWindowStart() == null ? null : stop.timeWindowStart().toSecondOfDay(),
                    stop.timeWindowEnd() == null ? null : stop.timeWindowEnd().toSecondOfDay()));
        }
        return new VrpProblem(matrix, vehicles, stops);
    }
}
