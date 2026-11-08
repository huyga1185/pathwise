package uk.huy.pathwise.routing.solver;

/**
 * One vehicle with its driver's shift. Times are seconds from 00:00 of the plan date, in the store's time zone (P-14).
 *
 * @param startNode         matrix node the route starts at; vehicles may share it (one depot in the MVP)
 * @param endNode           matrix node the route ends at; may differ from {@code startNode} (D-20, multi-depot later)
 * @param capacityUnit      what {@code capacity} counts (D-36), e.g. {@code "KG"}; the vehicle is not limited in other
 *                          units
 * @param capacity          load limit in {@code capacityUnit}
 * @param maxDistanceMeters route length limit, or null for none
 */
public record VrpVehicle(int startNode,
                         int endNode,
                         String capacityUnit,
                         int capacity,
                         int shiftStartSeconds,
                         int shiftEndSeconds,
                         Integer maxDistanceMeters) {}
