package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.job.service.JobHandler;
import uk.huy.pathwise.planning.model.*;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The short transactions of the geocoding job (ARCHITECTURE.md §8.2). All use {@code REQUIRES_NEW} because an inline
 * job runs right after the request's commit (see {@link uk.huy.pathwise.job.JobHandler}).
 */
@Service
@RequiredArgsConstructor
class PlanGeocodingService {
    private final RoutePlanRepository planRepository;

    record PendingStop(long id, String address) {}

    /**
     * Outcome of geocoding one stop: coordinates and display name when found, otherwise the error.
     */
    record Outcome(long stopId, String address, Double lat, Double lon, String displayName, GeocodeError error) {
        static Outcome found(PendingStop stop, double lat, double lon, String displayName) {
            return new Outcome(stop.id(), stop.address(), lat, lon, displayName, null);
        }

        static Outcome failed(PendingStop stop, GeocodeError error) {
            return new Outcome(stop.id(), stop.address(), null, null, null, error);
        }
    }

    /**
     * @return the PENDING stops of the plan, by id; empty if the plan does not exist
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public List<PendingStop> findPendingStops(long storeId, long planId) {
        return planRepository.findByIdAndStoreId(planId, storeId).stream()
                .flatMap(plan -> plan.getStops().stream())
                .filter(stop -> stop.getGeocodeStatus() == GeocodeStatus.PENDING)
                .map(stop -> new PendingStop(stop.getId(), stop.getAddressText()))
                .toList();
    }

    /**
     * Writes one batch. A stop that is no longer PENDING with the same address is left alone, so a result never
     * overwrites a newer change.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveOutcomes(long storeId, long planId, List<Outcome> outcomes) {
        RoutePlan plan = planRepository.findByIdAndStoreIdForUpdate(planId, storeId).orElse(null);
        if (plan == null) return;
        Map<Long, PlanStop> stops = plan.getStops().stream()
                .collect(Collectors.toMap(PlanStop::getId, Function.identity()));
        for (Outcome outcome : outcomes) {
            PlanStop stop = stops.get(outcome.stopId());
            if (stop == null || stop.getGeocodeStatus() != GeocodeStatus.PENDING
                    || !Objects.equals(stop.getAddressText(), outcome.address())) continue;
            stop.setLat(outcome.lat());
            stop.setLon(outcome.lon());
            stop.setGeocodedDisplayName(outcome.displayName());
            stop.setGeocodeError(outcome.error());
            stop.setGeocodeStatus(outcome.error() == null ? GeocodeStatus.RESOLVED : GeocodeStatus.FAILED);
        }
    }

    /**
     * Puts the plan back to IDLE if {@code jobId} is still its active job. Changing {@code processingStatus}
     * increments the plan version, once per job: the job has changed the stops (chosen by the owner in T29).
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
