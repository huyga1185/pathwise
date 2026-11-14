package uk.huy.pathwise.core.observability;

/**
 * What failed, for {@link FailureLog} (T54): the external services the app depends on, and background jobs.
 */
public enum FailureSource {
    /** The SMTP server refused or could not take an email. */
    EMAIL,
    /** The routing server (OSRM: matrix, directions). */
    ROUTING,
    /** The geocoding server (Nominatim). */
    GEOCODING,
    /** A background job ended as FAILED. */
    JOB
}
