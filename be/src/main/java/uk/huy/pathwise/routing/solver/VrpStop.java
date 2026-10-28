package uk.huy.pathwise.routing.solver;

import java.util.Map;

/**
 * One place to visit. Times are seconds from 00:00 of the plan date (P-14).
 *
 * @param node                   matrix node of the stop; each stop has its own node, never a vehicle's start or end
 * @param demands                demand per capacity unit (D-36), e.g. {@code {"KG": 12, "BOX": 2}}; a missing unit is
 *                               0. The caller adds {@code "ORDER": 1} for every stop when vehicles count orders.
 * @param timeWindowStartSeconds earliest start of service, or null (with the end) for no window
 * @param timeWindowEndSeconds   latest start of service, or null
 */
public record VrpStop(int node,
                      Map<String, Integer> demands,
                      int serviceTimeSeconds,
                      Integer timeWindowStartSeconds,
                      Integer timeWindowEndSeconds) {
    public int demand(String unit) {
        return demands.getOrDefault(unit, 0);
    }
}
