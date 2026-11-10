package uk.huy.pathwise.planning.model;

import java.time.Instant;

/**
 * The sharing state the driver's app last reported (P-53), for one route.
 */
public record LocationSharing(long routeId, LocationSharingState state, Instant updatedAt) {}
