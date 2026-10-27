package uk.huy.pathwise.routing.solver;

import uk.huy.pathwise.routing.matrix.TravelMatrix;

import java.util.List;

/**
 * Vehicle routing problem with capacities, time windows, shifts and route length limits (D-05, ARCHITECTURE.md §8.3).
 * A plain model: no entity, no Spring. Every matrix node is either the node of exactly one stop or a start/end node
 * of one or more vehicles.
 */
public record VrpProblem(TravelMatrix matrix, List<VrpVehicle> vehicles, List<VrpStop> stops) {}
