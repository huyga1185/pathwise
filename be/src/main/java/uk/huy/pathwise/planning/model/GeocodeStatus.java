package uk.huy.pathwise.planning.model;

/**
 * Where the coordinates of a stop come from (D-18).
 */
public enum GeocodeStatus {
    /** Given by the client; never geocoded. */
    PROVIDED,
    /** Address only, waiting for the geocoding job (T29). */
    PENDING,
    /** Coordinates found by geocoding the address. */
    RESOLVED,
    /** Geocoding gave no usable result; see the stop's {@link GeocodeError}. */
    FAILED
}
