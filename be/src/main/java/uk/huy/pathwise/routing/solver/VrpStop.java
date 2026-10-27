package uk.huy.pathwise.routing.solver;

/**
 * One place to visit. Times are seconds from 00:00 of the plan date (P-14).
 *
 * @param node                  matrix node of the stop; each stop has its own node, never a vehicle's start or end
 * @param timeWindowStartSeconds earliest start of service, or null (with the end) for no window
 * @param timeWindowEndSeconds   latest start of service, or null
 */
public record VrpStop(int node,
                      int demand,
                      int serviceTimeSeconds,
                      Integer timeWindowStartSeconds,
                      Integer timeWindowEndSeconds) {}
