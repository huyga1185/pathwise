package uk.huy.pathwise.job.model;

/**
 * Values of {@code Job.errorCode} set by the framework itself (API_SPEC §6). Handlers that fail on an external
 * provider get the provider's {@link uk.huy.pathwise.core.exception.ErrorCode} name instead, for example
 * {@code GEOCODING_PROVIDER_ERROR}.
 */
public final class JobErrors {
    /** The application stopped while the job was queued or running. */
    public static final String INTERRUPTED = "INTERRUPTED";
    /** The async queue was full when the job was submitted. */
    public static final String JOB_QUEUE_FULL = "JOB_QUEUE_FULL";
    /** Any other failure of the handler. */
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private JobErrors() {}
}
