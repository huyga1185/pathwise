package uk.huy.pathwise.planning.service;

import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.routing.model.Coordinate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * What the optimize job reads from the plan in its first transaction, so that the matrix and the solver run without
 * entities and outside any transaction (ARCHITECTURE.md §8.3).
 *
 * @param version the plan version read; the result is written only if the plan is still at it
 * @param routes  by route id, the order of the solver's vehicles
 * @param stops   by stop id, the order of the solver's stops
 */
record OptimizationInput(long version, LocalDate planDate, String timezone, List<Route> routes, List<Stop> stops) {
    record Route(long id,
                 Coordinate start,
                 Coordinate end,
                 CapacityUnit capacityUnit,
                 int capacity,
                 LocalTime shiftStart,
                 LocalTime shiftEnd,
                 Integer maxDistanceMeters) {}

    /**
     * @param timeWindowStart null with {@code timeWindowEnd} for no window
     */
    record Stop(long id,
                Coordinate location,
                Map<CapacityUnit, Integer> demands,
                int serviceTimeSeconds,
                LocalTime timeWindowStart,
                LocalTime timeWindowEnd) {}
}
