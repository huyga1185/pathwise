package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.planning.model.ProcessingStatus;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;

import java.util.Objects;

/**
 * Ends the processing of a plan for every plan job (geocode, optimize). {@code REQUIRES_NEW}, like every job-side
 * transaction (see {@link uk.huy.pathwise.job.service.JobHandler}).
 */
@Service
@RequiredArgsConstructor
class PlanProcessingService {
    private final RoutePlanRepository planRepository;

    /**
     * Puts the plan back to IDLE if {@code jobId} is still its active job, otherwise does nothing (the job already
     * released it). Changing {@code processingStatus} increments the plan version, once per job (P-30).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(long storeId, long planId, long jobId) {
        planRepository.findByIdAndStoreIdForUpdate(planId, storeId)
                .filter(plan -> Objects.equals(plan.getActiveJobId(), jobId))
                .ifPresent(plan -> {
                    plan.setProcessingStatus(ProcessingStatus.IDLE);
                    plan.setActiveJobId(null);
                });
    }
}
