package uk.huy.pathwise.planning.model;

/**
 * Job running on a plan (D-19). Anything but IDLE blocks edits, optimize and publish (BR-13).
 */
public enum ProcessingStatus {
    IDLE,
    GEOCODING,
    OPTIMIZING
}
