package uk.huy.pathwise.planning;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import uk.huy.pathwise.support.TestUsers.TestStore;

import java.util.concurrent.CountDownLatch;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GeocodePlanJobIntegrationTest} with {@code jobs.geocode-plan.mode=async} (the production default), plus what
 * only async shows: a plan stays busy while its job runs.
 */
@TestPropertySource(properties = "jobs.geocode-plan.mode=async")
class GeocodePlanJobAsyncIntegrationTest extends GeocodePlanJobIntegrationTest {
    @Test
    void edit_whileGeocoding_return409PlanBusy() throws Exception {
        TestStore store = storeWithDepot();
        String address = address("12 Lê Duẩn");
        CountDownLatch release = geocoding.hold(address);
        String body = create(store.owner(), "[{\"label\":\"A\",\"address\":\"" + address + "\"}]")
                .andReturn().getResponse().getContentAsString();
        long planId = id(body);
        long jobId = jobIdOf(body);
        try {
            await().until(() -> geocoding.calls(address) == 1);
            getJob(store.owner(), jobId).andExpect(jsonPath("$.data.status").value("RUNNING"));
            getPlan(store.owner(), planId)
                    .andExpect(jsonPath("$.data.processingStatus").value("GEOCODING"))
                    .andExpect(jsonPath("$.data.activeJob.status").value("RUNNING"));

            mockMvc.perform(put(PLANS + "/" + planId).header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"version\":0,\"name\":\"P\",\"planDate\":\"2026-11-02\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PLAN_BUSY"));
            mockMvc.perform(patch(PLANS + "/" + planId + "/stops/1").header(HttpHeaders.AUTHORIZATION, store.owner().bearer())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"version\":0,\"note\":\"x\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PLAN_BUSY"));
            geocode(store.owner(), planId, 0)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("PLAN_BUSY"));
        } finally {
            release.countDown();
        }
        awaitIdle(planId);
        getJob(store.owner(), jobId).andExpect(jsonPath("$.data.status").value("SUCCEEDED"));
    }
}
