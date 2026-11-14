package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.fleet.dto.FleetVehicle;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.fleet.service.VehicleQuery;
import uk.huy.pathwise.job.model.JobType;
import uk.huy.pathwise.job.service.JobService;
import uk.huy.pathwise.planning.dto.request.CopyPlanRequest;
import uk.huy.pathwise.planning.dto.response.PlanCopyResponse;
import uk.huy.pathwise.planning.dto.response.SkippedAssignmentResponse;
import uk.huy.pathwise.planning.dto.response.SkippedAssignmentResponse.Reason;
import uk.huy.pathwise.planning.model.*;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.store.dto.StoreProfile;
import uk.huy.pathwise.store.service.StoreMemberQuery;
import uk.huy.pathwise.store.service.StoreQuery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Copies a plan of any status into a new DRAFT ({@code POST /plans/{id}/copy}, T62, P-67, P-68). Stops keep their
 * location and geocoding result, so they are not geocoded again; a stop still PENDING geocoding starts the usual
 * geocoding job. Results, delivery status and the optimization are not copied. Assignments are copied only while the
 * driver and the vehicle may still be used (BR-07); the others are returned as skipped. Shift overlaps are checked at
 * publish (D-23), not here.
 */
@Service
@RequiredArgsConstructor
public class PlanCopyService {
    static final String COPY_SUFFIX = " (bản sao)";
    private static final int MAX_NAME = 150;

    private final RoutePlanRepository planRepository;
    private final StoreQuery storeQuery;
    private final StoreMemberQuery storeMemberQuery;
    private final VehicleQuery vehicleQuery;
    private final PlanDetailAssembler planDetailAssembler;
    private final JobService jobService;

    /**
     * @throws AppException {@code NOT_FOUND} for a plan outside the caller's store, {@code NOTHING_TO_COPY} if
     *                      {@code NOT_DELIVERED} leaves no stop
     */
    @Transactional
    public PlanCopyResponse copyPlan(UserIdentity actor, long planId, CopyPlanRequest request) {
        if (request == null) throw new NullPointerException("Copy plan request could not be null");
        if (actor == null || actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        long storeId = actor.storeId();
        RoutePlan source = planRepository.findByIdAndStoreId(planId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        List<PlanStop> stops = source.getStops().stream()
                .filter(stop -> request.stops() == CopyScope.ALL || stop.getStatus() != StopStatus.DELIVERED)
                .toList();
        if (request.stops() == CopyScope.NOT_DELIVERED && stops.isEmpty()) throw new AppException(ErrorCode.NOTHING_TO_COPY);
        StoreProfile store = storeQuery.findStore(storeId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        RoutePlan copy = RoutePlan.builder()
                .storeId(storeId)
                .name(nameOf(request, source))
                .planDate(request.planDate())
                .status(PlanStatus.DRAFT)
                .processingStatus(ProcessingStatus.IDLE)
                .depotAddress(source.getDepotAddress())
                .depotLat(source.getDepotLat())
                .depotLon(source.getDepotLon())
                .timezone(store.timezone())
                .createdBy(actor.id())
                .build();
        List<SkippedAssignmentResponse> skipped = new ArrayList<>();
        source.getRoutes().stream()
                .sorted(Comparator.comparing(PlanRoute::getId))
                .forEach(route -> copyRoute(copy, route).ifPresentOrElse(copy.getRoutes()::add,
                        () -> skipped.add(new SkippedAssignmentResponse(route.getDriverId(), route.getVehicleId(),
                                reasonOf(storeId, route)))));
        stops.forEach(stop -> copy.getStops().add(copyStop(copy, stop)));

        // GEOCODING before the insert, so that attaching the job afterwards does not move the version past 0.
        boolean geocode = copy.getStops().stream().anyMatch(stop -> stop.getGeocodeStatus() == GeocodeStatus.PENDING);
        if (geocode) copy.setProcessingStatus(ProcessingStatus.GEOCODING);
        planRepository.saveAndFlush(copy);
        if (geocode) {
            copy.setActiveJobId(jobService.createJob(storeId, JobType.GEOCODE_PLAN, copy.getId(), actor.id()));
            planRepository.flush();
        }
        return new PlanCopyResponse(planDetailAssembler.toDetail(copy), List.copyOf(skipped));
    }

    private static String nameOf(CopyPlanRequest request, RoutePlan source) {
        if (request.name() != null && !request.name().isBlank()) return request.name().trim();
        String base = source.getName();
        if (base.length() + COPY_SUFFIX.length() > MAX_NAME) base = base.substring(0, MAX_NAME - COPY_SUFFIX.length()).trim();
        return base + COPY_SUFFIX;
    }

    /**
     * @return the route for the copy, empty if its driver or vehicle may no longer be used; the capacity is the
     * vehicle's current one
     */
    private Optional<PlanRoute> copyRoute(RoutePlan copy, PlanRoute route) {
        if (!driverAvailable(copy.getStoreId(), route.getDriverId())) return Optional.empty();
        Optional<FleetVehicle> vehicle = vehicleQuery.findActive(copy.getStoreId(), route.getVehicleId());
        return vehicle.map(active -> PlanRoute.builder()
                .plan(copy)
                .storeId(copy.getStoreId())
                .driverId(route.getDriverId())
                .vehicleId(active.id())
                .vehicleCapacity(active.capacity())
                .vehicleCapacityUnit(active.capacityUnit())
                .shiftStart(route.getShiftStart())
                .shiftEnd(route.getShiftEnd())
                .maxDistanceMeters(route.getMaxDistanceMeters())
                .startAddress(copy.getDepotAddress())
                .startLat(copy.getDepotLat())
                .startLon(copy.getDepotLon())
                .endAddress(copy.getDepotAddress())
                .endLat(copy.getDepotLat())
                .endLon(copy.getDepotLon())
                .build());
    }

    private Reason reasonOf(long storeId, PlanRoute route) {
        return driverAvailable(storeId, route.getDriverId()) ? Reason.VEHICLE_INACTIVE : Reason.DRIVER_UNAVAILABLE;
    }

    private boolean driverAvailable(long storeId, long driverId) {
        return storeMemberQuery.findActiveMember(storeId, driverId)
                .filter(member -> UserIdentity.canBeAssignedRoutes(member.role(), member.canDrive()))
                .isPresent();
    }

    private static PlanStop copyStop(RoutePlan copy, PlanStop stop) {
        Map<CapacityUnit, Integer> demands = new EnumMap<>(CapacityUnit.class);
        demands.putAll(stop.getDemands());
        return PlanStop.builder()
                .plan(copy)
                .storeId(copy.getStoreId())
                .label(stop.getLabel())
                .recipientName(stop.getRecipientName())
                .recipientPhone(stop.getRecipientPhone())
                .addressText(stop.getAddressText())
                .lat(stop.getLat())
                .lon(stop.getLon())
                .geocodeStatus(stop.getGeocodeStatus())
                .geocodeError(stop.getGeocodeError())
                .geocodedDisplayName(stop.getGeocodedDisplayName())
                .demands(demands)
                .serviceTimeSeconds(stop.getServiceTimeSeconds())
                .timeWindowStart(stop.getTimeWindowStart())
                .timeWindowEnd(stop.getTimeWindowEnd())
                .note(stop.getNote())
                .status(StopStatus.PENDING)
                .build();
    }
}
