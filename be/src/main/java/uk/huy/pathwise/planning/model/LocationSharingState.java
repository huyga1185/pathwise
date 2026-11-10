package uk.huy.pathwise.planning.model;

/**
 * Whether the driver's app is sending its location for a route, as the app reports it (P-53).
 */
public enum LocationSharingState {
    /** The driver agreed and the app is sending. */
    SHARING,
    /** The driver chose not to share for this route. */
    DECLINED,
    /** The phone does not let the app use the location. */
    NO_PERMISSION,
    /** Location (GPS) is turned off on the phone. */
    GPS_OFF
}
