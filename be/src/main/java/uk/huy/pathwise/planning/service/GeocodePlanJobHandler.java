package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.ExternalServiceException;
import uk.huy.pathwise.job.dto.JobRef;
import uk.huy.pathwise.job.model.JobType;
import uk.huy.pathwise.job.service.JobHandler;
import uk.huy.pathwise.planning.infrastructure.config.GeocodePlanProperties;
import uk.huy.pathwise.planning.model.GeocodeError;
import uk.huy.pathwise.planning.service.PlanGeocodingService.Outcome;
import uk.huy.pathwise.planning.service.PlanGeocodingService.PendingStop;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingResult;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code GEOCODE_PLAN} (FR-P09, ARCHITECTURE.md §8.2): geocodes the PENDING stops of a plan outside any transaction
 * and writes the results in small batches. A stop that cannot be geocoded fails on its own (NOT_FOUND or
 * PROVIDER_ERROR) and the job goes on, because that is a data problem, not a job failure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GeocodePlanJobHandler implements JobHandler {
    /** Attempts per stop while the shared rate limit has no free slot (503 GEOCODING_BUSY). */
    static final int BUSY_ATTEMPTS = 3;

    private final PlanGeocodingService planGeocodingService;
    private final GeocodingProvider geocodingProvider;
    private final GeocodePlanProperties properties;

    @Override
    public JobType type() {
        return JobType.GEOCODE_PLAN;
    }

    @Override
    public void run(JobRef job) {
        try {
            List<PendingStop> stops = planGeocodingService.findPendingStops(job.storeId(), job.targetId());
            List<Outcome> batch = new ArrayList<>();
            for (PendingStop stop : stops) {
                batch.add(geocode(stop));
                if (batch.size() == properties.batchSize()) {
                    planGeocodingService.saveOutcomes(job.storeId(), job.targetId(), batch);
                    batch = new ArrayList<>();
                }
            }
            if (!batch.isEmpty()) planGeocodingService.saveOutcomes(job.storeId(), job.targetId(), batch);
        } finally {
            planGeocodingService.release(job.storeId(), job.targetId(), job.id());
        }
    }

    @Override
    public void abandon(JobRef job) {
        planGeocodingService.release(job.storeId(), job.targetId(), job.id());
    }

    private Outcome geocode(PendingStop stop) {
        for (int attempt = 1; ; attempt++) {
            try {
                List<GeocodingResult> results = geocodingProvider.search(stop.address(), 1);
                if (results.isEmpty()) return Outcome.failed(stop, GeocodeError.NOT_FOUND);
                GeocodingResult best = results.getFirst();
                return Outcome.found(stop, best.lat(), best.lon(), best.displayName());
            } catch (ExternalServiceException e) {
                // Busy means other requests hold the rate limit slots: wait for our turn again, a few times.
                if (e.getErrorCode() == ErrorCode.GEOCODING_BUSY && attempt < BUSY_ATTEMPTS) continue;
                log.warn("Geocoding a stop failed. stopId={}, errorCode={}", stop.id(), e.getErrorCode());
                return Outcome.failed(stop, GeocodeError.PROVIDER_ERROR);
            }
        }
    }
}
