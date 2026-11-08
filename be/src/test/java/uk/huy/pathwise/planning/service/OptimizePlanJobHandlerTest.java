package uk.huy.pathwise.planning.service;

import org.junit.jupiter.api.Test;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.model.JobType;
import uk.huy.pathwise.job.service.JobFailedException;
import uk.huy.pathwise.routing.matrix.MatrixProvider;
import uk.huy.pathwise.routing.matrix.TravelMatrix;
import uk.huy.pathwise.routing.model.Coordinate;
import uk.huy.pathwise.routing.solver.SolverProvider;
import uk.huy.pathwise.routing.solver.VrpProblem;
import uk.huy.pathwise.routing.solver.VrpSolution;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OptimizePlanJobHandlerTest {
    private static final JobRef JOB = new JobRef(9, JobType.OPTIMIZE_PLAN, 1, 7);
    private static final Coordinate DEPOT = new Coordinate(16.05, 108.2);

    private final PlanOptimizationService optimization = mock(PlanOptimizationService.class);
    private final PlanProcessingService processing = mock(PlanProcessingService.class);
    private final MatrixProvider matrixProvider = mock(MatrixProvider.class);
    private final SolverProvider solverProvider = mock(SolverProvider.class);
    private final OptimizePlanJobHandler handler = new OptimizePlanJobHandler(optimization, processing, matrixProvider, solverProvider);

    @Test
    void run_whenSolverFindsNoSolution_failNoSolutionKeepResultAndRelease() {
        when(optimization.loadInput(1, 7)).thenReturn(Optional.of(input(CapacityUnit.BOX)));
        when(matrixProvider.matrix(anyList())).thenReturn(new TravelMatrix(new int[2][2], new int[2][2]));
        when(solverProvider.solve(any())).thenReturn(new VrpSolution(VrpSolution.Status.NO_SOLUTION, List.of(), List.of(0)));

        assertThatThrownBy(() -> handler.run(JOB))
                .isInstanceOf(JobFailedException.class)
                .extracting("errorCode").isEqualTo("NO_SOLUTION");
        verify(optimization, never()).saveResult(anyLong(), anyLong(), anyLong(), any(), any());
        verify(processing).release(1, 7, 9);
    }

    @Test
    void run_whenVehicleCountsOrders_addOneOrderToEveryStopAndShareTheDepotNode() {
        when(optimization.loadInput(1, 7)).thenReturn(Optional.of(input(CapacityUnit.ORDER)));
        when(matrixProvider.matrix(anyList())).thenReturn(new TravelMatrix(new int[2][2], new int[2][2]));
        when(solverProvider.solve(any())).thenReturn(new VrpSolution(VrpSolution.Status.SOLVED, List.of(), List.of()));

        handler.run(JOB);

        verify(matrixProvider).matrix(List.of(DEPOT, new Coordinate(16.06, 108.21)));
        verify(solverProvider).solve(argThat((VrpProblem problem) ->
                problem.vehicles().getFirst().startNode() == 0 && problem.vehicles().getFirst().endNode() == 0
                        && problem.vehicles().getFirst().shiftStartSeconds() == 8 * 3600
                        && problem.stops().getFirst().node() == 1
                        && problem.stops().getFirst().demands().equals(Map.of("BOX", 2, "ORDER", 1))
                        && problem.stops().getFirst().timeWindowStartSeconds() == 9 * 3600));
        verify(optimization).saveResult(eq(1L), eq(7L), eq(9L), any(), any());
    }

    @Test
    void run_whenPlanGone_doNothingButRelease() {
        when(optimization.loadInput(1, 7)).thenReturn(Optional.empty());

        handler.run(JOB);

        verifyNoInteractions(matrixProvider, solverProvider);
        verify(processing).release(1, 7, 9);
        assertThat(handler.type()).isEqualTo(JobType.OPTIMIZE_PLAN);
    }

    private static OptimizationInput input(CapacityUnit vehicleUnit) {
        return new OptimizationInput(3, LocalDate.of(2026, 11, 2), "Asia/Ho_Chi_Minh",
                List.of(new OptimizationInput.Route(11, DEPOT, DEPOT, vehicleUnit, 5, LocalTime.of(8, 0), LocalTime.of(17, 0), null)),
                List.of(new OptimizationInput.Stop(101, new Coordinate(16.06, 108.21), Map.of(CapacityUnit.BOX, 2), 0,
                        LocalTime.of(9, 0), LocalTime.of(10, 0))));
    }
}
