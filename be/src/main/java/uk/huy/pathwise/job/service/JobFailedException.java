package uk.huy.pathwise.job.service;

import lombok.Getter;

/**
 * Thrown by a {@link JobHandler} to fail its job with a specific {@code Job.errorCode} that is not an
 * {@link uk.huy.pathwise.core.exception.ErrorCode}, for example {@code NO_SOLUTION} (API_SPEC §6).
 */
@Getter
public class JobFailedException extends RuntimeException {
    private final String errorCode;

    public JobFailedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
