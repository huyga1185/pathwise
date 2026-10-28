package uk.huy.pathwise.job.dto;

import java.time.Instant;
import uk.huy.pathwise.job.model.JobErrors;
import uk.huy.pathwise.job.model.JobStatus;
import uk.huy.pathwise.job.model.JobType;

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
                          String errorCode,
                          Instant createdAt,
                          Instant startedAt,
                          Instant finishedAt) {}
