package uk.huy.pathwise.job.dto;

import uk.huy.pathwise.job.model.JobType;
/**
 * A job as handlers see it; the entity stays inside the {@code job} module (ARCHITECTURE.md §2.1).
 *
 * @param targetId the plan id for every current type
 */
public record JobRef(long id, JobType type, long storeId, long targetId) {}
