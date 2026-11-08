package uk.huy.pathwise.job.dto;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.job.model.JobErrors;
import uk.huy.pathwise.job.model.JobStatus;
import uk.huy.pathwise.job.model.JobType;

import java.time.Instant;

/**
 * {@code Job} of API_SPEC §6.
 *
 * @param planId    the target of the job; every current type targets a plan
 * @param errorCode set when FAILED: a {@link JobErrors} value or a provider error code
 */
public record JobResponse(long id,
                          JobType type,
                          JobStatus status,
                          long planId,
                          @Nullable String errorCode,
                          Instant createdAt,
                          @Nullable Instant startedAt,
                          @Nullable Instant finishedAt) {}
