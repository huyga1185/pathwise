package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.model.JobType;
import uk.huy.pathwise.job.service.JobFailedException;
import uk.huy.pathwise.job.service.JobHandler;
import uk.huy.pathwise.routing.matrix.MatrixProvider;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.solver.SolverProvider;
import uk.huy.pathwise.routing.solver.VrpSolution;

/**
 * {@code OPTIMIZE_PLAN} (FR-P04, ARCHITECTURE.md §8.3) in three steps: read the input in a transaction, build the
 * matrix and solve outside any transaction, write the result in a transaction that checks the plan version.
 * A provider failure fails the job ({@code ROUTING_PROVIDER_ERROR}) and keeps the previous result.
 */
@Component
@RequiredArgsConstructor
class OptimizePlanJobHandler implements JobHandler {
    /** {@code Job.errorCode} when the solver finds no solution at all; the previous result is kept. */
    static final String NO_SOLUTION = "NO_SOLUTION";

    private final PlanOptimizationService planOptimizationService;
    private final PlanProcessingService planProcessingService;
    private final MatrixProvider matrixProvider;
    private final SolverProvider solverProvider;

    @Override
    public JobType type() {
        return JobType.OPTIMIZE_PLAN;
    }

    @Override
    public void run(JobRef job) {
        try {
            OptimizationInput input = planOptimizationService.loadInput(job.storeId(), job.targetId()).orElse(null);
            if (input == null) return;
            OptimizationModel model = OptimizationModel.of(input);
            TravelMatrix matrix = matrixProvider.matrix(model.nodes());
            VrpSolution solution = solverProvider.solve(model.toProblem(matrix));
            if (solution.status() == VrpSolution.Status.NO_SOLUTION)
                throw new JobFailedException(NO_SOLUTION, "The solver found no solution");
            planOptimizationService.saveResult(job.storeId(), job.targetId(), job.id(), input, solution);
        } finally {
            // No-op after a saved result, which already released the plan.
            planProcessingService.release(job.storeId(), job.targetId(), job.id());
        }
    }

    @Override
    public void abandon(JobRef job) {
        planProcessingService.release(job.storeId(), job.targetId(), job.id());
    }
}
