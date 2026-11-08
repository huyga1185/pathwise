package uk.huy.pathwise.planning;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import uk.huy.pathwise.support.TestUsers.TestStore;

import java.util.concurrent.CountDownLatch;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link OptimizePlanIntegrationTest} with {@code jobs.optimize-plan.mode=async}, plus what only async shows: the plan
 * stays busy while it is being optimized.
 */
@TestPropertySource(properties = "jobs.optimize-plan.mode=async")
class OptimizePlanAsyncIntegrationTest extends OptimizePlanIntegrationTest {
    @Test
    void edit_whileOptimizing_return409PlanBusy() throws Exception {
        TestStore store = storeWithDepot();
        long vehicle = createVehicle(store.owner(), "Xe 1", 10, "BOX");
        long planId = createPlan(store.owner(), "[{\"label\":\"A\",\"lat\":16.06,\"lon\":108.21}]", assignment(store.driver().id(), vehicle));
        CountDownLatch release = matrix.holdNext();
        try {
            optimize(store.owner(), planId, 0).andExpect(status().isAccepted());
            await().until(() -> processingStatus(planId).equals("OPTIMIZING"));
            getPlan(store.owner(), planId).andExpect(jsonPath("$.data.activeJob.type").value("OPTIMIZE_PLAN"));

            mockMvc.perform(put(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"version\":1,\"name\":\"P\",\"planDate\":\"2026-11-02\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PLAN_BUSY"));
            optimize(store.owner(), planId, 1)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PLAN_BUSY"));
        } finally {
            release.countDown();
        }
        awaitIdle(planId);
    }
}
