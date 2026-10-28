package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.DetailedAppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.FieldErrorDetail;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.core.pagination.PaginationValidator;
import uk.huy.pathwise.core.phonenumber.PhoneNumberProcessor;
import uk.huy.pathwise.fleet.dto.FleetVehicle;
import uk.huy.pathwise.fleet.model.CapacityUnit;
import uk.huy.pathwise.fleet.service.VehicleQuery;
import uk.huy.pathwise.job.dto.JobResponse;
import uk.huy.pathwise.job.model.JobType;
import uk.huy.pathwise.job.service.JobService;
import uk.huy.pathwise.planning.dto.request.*;
import uk.huy.pathwise.planning.dto.response.PlanDetailResponse;
import uk.huy.pathwise.planning.dto.response.PlanSummaryResponse;
import uk.huy.pathwise.planning.infrastructure.mapper.PlanMapper;
import uk.huy.pathwise.planning.model.*;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.store.dto.StoreMember;
import uk.huy.pathwise.store.dto.StoreProfile;
import uk.huy.pathwise.store.service.StoreMemberQuery;
import uk.huy.pathwise.store.service.StoreQuery;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Plan drafts of the caller's store (FR-P02, FR-P03, FR-P07). The store always comes from the access token; a plan of
 * another store is reported as not found (BR-01).
 * <p>
 * Every change locks the plan row, checks the {@code version} sent by the client (409 {@code PLAN_MODIFIED}) and
 * removes the optimization result (FR-P03). A save that leaves stops PENDING (address only) starts a
 * {@code GEOCODE_PLAN} job and marks the plan GEOCODING (D-18); the job runs after the commit.
 */
@Service
@RequiredArgsConstructor
public class PlanService {
    /** D-19. */
    static final int MAX_STOPS = 100;
    /** D-19. */
    static final int MAX_ASSIGNMENTS = 20;
    static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORT_WHITELIST = Set.of("planDate", "createdAt", "name");

    private final RoutePlanRepository planRepository;
    private final StoreQuery storeQuery;
    private final StoreMemberQuery storeMemberQuery;
    private final VehicleQuery vehicleQuery;
    private final PhoneNumberProcessor phoneNumberProcessor;
    private final PlanMapper planMapper;
    private final PlanDetailAssembler planDetailAssembler;
    private final JobService jobService;
    private final TransactionTemplate transactionTemplate;

    /**
     * @throws AppException {@code PLAN_TOO_LARGE}, {@code STORE_DEPOT_REQUIRED}, {@code INVALID_ASSIGNMENT}
     */
    @Transactional
    public PlanDetailResponse createPlan(UserIdentity actor, PlanDraftRequest request) {
        if (request == null) throw new NullPointerException("Plan draft request could not be null");
        long storeId = storeIdOf(actor);
        checkSize(request);
        RoutePlan plan = RoutePlan.builder()
                .storeId(storeId)
                .status(PlanStatus.DRAFT)
                .processingStatus(ProcessingStatus.IDLE)
                .createdBy(actor.id())
                .build();
        applyDraft(plan, request, Map.of());
        // GEOCODING before the insert, so that attaching the job afterwards does not move the version past 0.
        boolean geocode = markGeocodingIfPending(plan);
        planRepository.saveAndFlush(plan);
        if (geocode) {
            attachJob(plan, actor, JobType.GEOCODE_PLAN);
            planRepository.flush();
        }
        return planDetailAssembler.toDetail(plan);
    }

    /**
     * @param planDate only plans of this day, if set
     * @param status   only plans with this status, if set
     * @param pageable sorted by {@code planDate} (newest first) by default; size at most {@value #MAX_PAGE_SIZE}
     * @throws AppException {@code INVALID_SORT_FIELD} for a sort outside {@code planDate}, {@code createdAt},
     *                      {@code name}
     */
    @Transactional(readOnly = true)
    public PageResponse<PlanSummaryResponse> listPlans(UserIdentity actor, LocalDate planDate, PlanStatus status, Pageable pageable) {
        long storeId = storeIdOf(actor);
        PaginationValidator.validateSort(pageable, SORT_WHITELIST);
        // The id makes the order stable between pages of plans with the same sort value.
        Pageable page = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id")));
        Page<RoutePlan> plans = planRepository.findPage(storeId, planDate, status, page);
        List<Long> ids = plans.getContent().stream().map(RoutePlan::getId).toList();
        Map<Long, Long> stopCounts = ids.isEmpty() ? Map.of() : toCounts(planRepository.countStops(ids));
        Map<Long, Long> routeCounts = ids.isEmpty() ? Map.of() : toCounts(planRepository.countRoutes(ids));
        return PageResponse.from(plans.map(plan -> planMapper.toSummaryResponse(plan,
                stopCounts.getOrDefault(plan.getId(), 0L),
                routeCounts.getOrDefault(plan.getId(), 0L))));
    }

    /**
     * @throws AppException {@code NOT_FOUND} if the plan is not in the caller's store
     */
    @Transactional(readOnly = true)
    public PlanDetailResponse getPlan(UserIdentity actor, long planId) {
        RoutePlan plan = planRepository.findByIdAndStoreId(planId, storeIdOf(actor))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return planDetailAssembler.toDetail(plan);
    }

    /**
     * Replaces the whole draft. A stop sent with the {@code id} of one of the plan's stops keeps its geocoding result
     * while its address and coordinates do not change; stops left out are removed.
     *
     * @throws AppException {@code NOT_FOUND}, {@code PLAN_NOT_EDITABLE}, {@code PLAN_BUSY}, {@code PLAN_MODIFIED},
     *                      {@code PLAN_TOO_LARGE}, {@code STORE_DEPOT_REQUIRED}, {@code INVALID_ASSIGNMENT}, and
     *                      {@code VALIDATION_ERROR} with {@code INVALID_STOP_ID} for an id that is not a stop of the
     *                      plan or appears twice
     */
    @Transactional
    public PlanDetailResponse replacePlan(UserIdentity actor, long planId, PlanDraftRequest request) {
        if (request == null) throw new NullPointerException("Plan draft request could not be null");
        RoutePlan plan = lockEditablePlan(actor, planId, request.version());
        checkSize(request);
        Map<Long, PlanStop> existing = plan.getStops().stream()
                .collect(Collectors.toMap(PlanStop::getId, Function.identity()));
        checkStopIds(request.stopsOrEmpty(), existing.keySet());
        applyDraft(plan, request, existing);
        if (markGeocodingIfPending(plan)) attachJob(plan, actor, JobType.GEOCODE_PLAN);
        markModified(plan);
        planRepository.flush();
        return planDetailAssembler.toDetail(plan);
    }

    /**
     * Changes one stop with merge-patch semantics: an absent field keeps its value, a {@code null} one clears it (see
     * {@link PatchStopRequest}). New coordinates make the stop {@code PROVIDED}; a new address without coordinates,
     * or cleared coordinates, make it {@code PENDING} again.
     *
     * @throws AppException {@code NOT_FOUND} for a plan or stop outside the caller's store, {@code PLAN_NOT_EDITABLE},
     *                      {@code PLAN_BUSY}, {@code PLAN_MODIFIED}, and {@code VALIDATION_ERROR} with
     *                      {@code ADDRESS_OR_COORDINATES_REQUIRED} if the stop would have neither
     */
    @Transactional
    public PlanDetailResponse patchStop(UserIdentity actor, long planId, long stopId, PatchStopRequest request) {
        if (request == null) throw new NullPointerException("Patch stop request could not be null");
        RoutePlan plan = lockEditablePlan(actor, planId, request.getVersion());
        PlanStop stop = plan.getStops().stream()
                .filter(candidate -> candidate.getId() == stopId)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        applyPatch(stop, request);
        clearOptimization(plan);
        if (markGeocodingIfPending(plan)) attachJob(plan, actor, JobType.GEOCODE_PLAN);
        markModified(plan);
        planRepository.flush();
        return planDetailAssembler.toDetail(plan);
    }

    /**
     * Geocodes again every PENDING or FAILED stop ({@code POST /plans/{id}/geocode}): FAILED stops go back to PENDING
     * and a {@code GEOCODE_PLAN} job starts.
     *
     * @return the job, read after the commit: an inline job has already run, so its final status is returned
     * @throws AppException {@code NOT_FOUND}, {@code PLAN_NOT_EDITABLE}, {@code PLAN_BUSY}, {@code PLAN_MODIFIED},
     *                      {@code NOTHING_TO_GEOCODE} if no stop is PENDING or FAILED
     */
    public JobResponse geocodePlan(UserIdentity actor, long planId, PlanVersionRequest request) {
        if (request == null) throw new NullPointerException("Geocode plan request could not be null");
        long storeId = storeIdOf(actor);
        // Not @Transactional: the job is submitted when this transaction commits, and is read after that.
        Long jobId = transactionTemplate.execute(status -> {
            RoutePlan plan = lockEditablePlan(actor, planId, request.version());
            List<PlanStop> stops = plan.getStops().stream()
                    .filter(stop -> stop.getGeocodeStatus() == GeocodeStatus.PENDING
                            || stop.getGeocodeStatus() == GeocodeStatus.FAILED)
                    .toList();
            if (stops.isEmpty()) throw new AppException(ErrorCode.NOTHING_TO_GEOCODE);
            for (PlanStop stop : stops) {
                stop.setGeocodeStatus(GeocodeStatus.PENDING);
                stop.setGeocodeError(null);
                stop.setGeocodedDisplayName(null);
            }
            clearOptimization(plan);
            markGeocodingIfPending(plan);
            attachJob(plan, actor, JobType.GEOCODE_PLAN);
            markModified(plan);
            return plan.getActiveJobId();
        });
        return jobService.findJob(storeId, Objects.requireNonNull(jobId)).orElseThrow();
    }

    /**
     * Starts an {@code OPTIMIZE_PLAN} job ({@code POST /plans/{id}/optimize}, ARCHITECTURE.md §8.3). The plan is
     * OPTIMIZING until the job ends; the job writes the result only if the plan is still at the version it read.
     *
     * @return the job, read after the commit: an inline job (the default) has already run, so its final status is
     * returned
     * @throws AppException {@code NOT_FOUND}, {@code PLAN_NOT_EDITABLE}, {@code PLAN_BUSY}, {@code PLAN_MODIFIED},
     *                      {@code PLAN_INCOMPLETE} without stops or assignments, {@code STOPS_NOT_GEOCODED} while a stop
     *                      has no coordinates, {@code INVALID_ASSIGNMENT} if a driver or vehicle may no longer be used
     *                      (BR-07), each with the assignment in {@code details}
     */
    public JobResponse optimizePlan(UserIdentity actor, long planId, PlanVersionRequest request) {
        if (request == null) throw new NullPointerException("Optimize plan request could not be null");
        long storeId = storeIdOf(actor);
        // Not @Transactional: the job is submitted when this transaction commits, and is read after that.
        Long jobId = transactionTemplate.execute(status -> {
            RoutePlan plan = lockEditablePlan(actor, planId, request.version());
            checkComplete(plan);
            if (plan.getStops().stream().anyMatch(stop -> stop.getLat() == null))
                throw new AppException(ErrorCode.STOPS_NOT_GEOCODED);
            checkAssignmentsStillValid(plan);
            plan.setProcessingStatus(ProcessingStatus.OPTIMIZING);
            attachJob(plan, actor, JobType.OPTIMIZE_PLAN);
            markModified(plan);
            return plan.getActiveJobId();
        });
        return jobService.findJob(storeId, Objects.requireNonNull(jobId)).orElseThrow();
    }

    private static void checkComplete(RoutePlan plan) {
        List<FieldErrorDetail> missing = new ArrayList<>();
        if (plan.getStops().isEmpty()) missing.add(new FieldErrorDetail("stops", "STOPS_REQUIRED"));
        if (plan.getRoutes().isEmpty()) missing.add(new FieldErrorDetail("assignments", "ASSIGNMENTS_REQUIRED"));
        if (!missing.isEmpty()) throw new DetailedAppException(ErrorCode.PLAN_INCOMPLETE, missing);
    }

    /**
     * BR-07 again at optimize time: since the draft was saved a driver may have been disabled or lost
     * {@code canDrive}, and a vehicle may have been deactivated. Assignments are numbered like
     * {@code PlanDetail.routes}.
     */
    private void checkAssignmentsStillValid(RoutePlan plan) {
        List<PlanRoute> routes = plan.getRoutes().stream().sorted(Comparator.comparing(PlanRoute::getId)).toList();
        List<FieldErrorDetail> invalid = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            PlanRoute route = routes.get(i);
            boolean driverValid = storeMemberQuery.findActiveMember(plan.getStoreId(), route.getDriverId())
                    .filter(member -> UserIdentity.canBeAssignedRoutes(member.role(), member.canDrive()))
                    .isPresent();
            if (!driverValid) invalid.add(new FieldErrorDetail("assignments[" + i + "].driverId", "INVALID_ASSIGNMENT"));
            if (vehicleQuery.findActive(plan.getStoreId(), route.getVehicleId()).isEmpty())
                invalid.add(new FieldErrorDetail("assignments[" + i + "].vehicleId", "INVALID_ASSIGNMENT"));
        }
        if (!invalid.isEmpty()) throw new DetailedAppException(ErrorCode.INVALID_ASSIGNMENT, invalid);
    }

    private RoutePlan lockEditablePlan(UserIdentity actor, long planId, Long version) {
        RoutePlan plan = planRepository.findByIdAndStoreIdForUpdate(planId, storeIdOf(actor))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (plan.getStatus() != PlanStatus.DRAFT) throw new AppException(ErrorCode.PLAN_NOT_EDITABLE);
        if (plan.getProcessingStatus() != ProcessingStatus.IDLE) throw new AppException(ErrorCode.PLAN_BUSY);
        if (version == null || version != plan.getVersion()) throw new AppException(ErrorCode.PLAN_MODIFIED);
        return plan;
    }

    private static void checkSize(PlanDraftRequest request) {
        if (request.stopsOrEmpty().size() > MAX_STOPS || request.assignmentsOrEmpty().size() > MAX_ASSIGNMENTS)
            throw new AppException(ErrorCode.PLAN_TOO_LARGE);
    }

    private static void checkStopIds(List<PlanStopRequest> stops, Set<Long> existingIds) {
        Set<Long> seen = new HashSet<>();
        List<FieldErrorDetail> errors = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            Long id = stops.get(i).id();
            // Same error for an unknown id, another plan's stop and a duplicate: nothing about other plans leaks.
            if (id != null && (!existingIds.contains(id) || !seen.add(id)))
                errors.add(new FieldErrorDetail("stops[" + i + "].id", "INVALID_STOP_ID"));
        }
        if (!errors.isEmpty()) throw new DetailedAppException(ErrorCode.VALIDATION_ERROR, errors);
    }

    /**
     * Writes the whole draft into {@code plan}: name, date, depot and timezone snapshots, the assignments (routes are
     * recreated), and the stops ({@code existing} by id are updated in place, the others removed).
     */
    private void applyDraft(RoutePlan plan, PlanDraftRequest request, Map<Long, PlanStop> existing) {
        if (!plan.getRoutes().isEmpty()) {
            // Delete the old routes before inserting the new ones: (plan_id, driver_id) and (plan_id, vehicle_id) are
            // unique, and the same driver or vehicle usually stays in the plan. Flushed before any field of the plan
            // changes, so that this flush does not increment the version too.
            plan.getStops().forEach(stop -> stop.setRoute(null));
            plan.getRoutes().clear();
            planRepository.flush();
        }
        StoreProfile store = storeQuery.findStore(plan.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        plan.setName(request.name().trim());
        plan.setPlanDate(request.planDate());
        applyDepot(plan, request.depot(), store);
        plan.setTimezone(store.timezone());
        plan.getRoutes().addAll(buildRoutes(plan, request.assignmentsOrEmpty()));
        clearOptimization(plan);

        List<PlanStop> stops = new ArrayList<>();
        for (PlanStopRequest stopRequest : request.stopsOrEmpty()) {
            PlanStop stop = stopRequest.id() == null ? newStop(plan) : existing.get(stopRequest.id());
            applyStop(stop, stopRequest);
            stops.add(stop);
        }
        plan.getStops().retainAll(stops);
        stops.stream().filter(stop -> stop.getId() == null).forEach(plan.getStops()::add);
    }

    /**
     * @throws AppException {@code STORE_DEPOT_REQUIRED} if no depot is given and the store has none
     */
    private static void applyDepot(RoutePlan plan, DepotRequest depot, StoreProfile store) {
        if (depot != null) {
            plan.setDepotAddress(trimToNull(depot.address()));
            plan.setDepotLat(depot.lat());
            plan.setDepotLon(depot.lon());
            return;
        }
        if (store.depotLat() == null || store.depotLon() == null) throw new AppException(ErrorCode.STORE_DEPOT_REQUIRED);
        plan.setDepotAddress(store.depotAddress());
        plan.setDepotLat(store.depotLat());
        plan.setDepotLon(store.depotLon());
    }

    /**
     * BR-07: each driver and each vehicle at most once; the driver an ACTIVE member of the store who may drive (D-21);
     * the vehicle ACTIVE and of the store. Start and end are the plan depot (D-20).
     *
     * @throws AppException {@code INVALID_ASSIGNMENT} if any assignment breaks these rules
     */
    private List<PlanRoute> buildRoutes(RoutePlan plan, List<AssignmentRequest> assignments) {
        long storeId = plan.getStoreId();
        Set<Long> driverIds = new HashSet<>();
        Set<Long> vehicleIds = new HashSet<>();
        List<PlanRoute> routes = new ArrayList<>();
        for (AssignmentRequest assignment : assignments) {
            if (!driverIds.add(assignment.driverId()) || !vehicleIds.add(assignment.vehicleId()))
                throw new AppException(ErrorCode.INVALID_ASSIGNMENT);
            StoreMember driver = storeMemberQuery.findActiveMember(storeId, assignment.driverId())
                    .filter(member -> UserIdentity.canBeAssignedRoutes(member.role(), member.canDrive()))
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_ASSIGNMENT));
            FleetVehicle vehicle = vehicleQuery.findActive(storeId, assignment.vehicleId())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_ASSIGNMENT));
            routes.add(PlanRoute.builder()
                    .plan(plan)
                    .storeId(storeId)
                    .driverId(driver.id())
                    .vehicleId(vehicle.id())
                    .vehicleCapacity(vehicle.capacity())
                    .vehicleCapacityUnit(vehicle.capacityUnit())
                    .shiftStart(assignment.shiftStart())
                    .shiftEnd(assignment.shiftEnd())
                    .maxDistanceMeters(assignment.maxDistanceMeters())
                    .startAddress(plan.getDepotAddress())
                    .startLat(plan.getDepotLat())
                    .startLon(plan.getDepotLon())
                    .endAddress(plan.getDepotAddress())
                    .endLat(plan.getDepotLat())
                    .endLon(plan.getDepotLon())
                    .build());
        }
        return routes;
    }

    private static PlanStop newStop(RoutePlan plan) {
        return PlanStop.builder()
                .plan(plan)
                .storeId(plan.getStoreId())
                .status(StopStatus.PENDING)
                .build();
    }

    private void applyStop(PlanStop stop, PlanStopRequest request) {
        stop.setLabel(request.label().trim());
        stop.setRecipientName(trimToNull(request.recipientName()));
        stop.setRecipientPhone(toE164(request.recipientPhone()));
        setDemands(stop, request.demands());
        stop.setServiceTimeSeconds(request.serviceTimeSeconds() == null ? 0 : request.serviceTimeSeconds());
        setTimeWindow(stop, request.timeWindow());
        stop.setNote(trimToNull(request.note()));
        applyLocation(stop, trimToNull(request.address()), request.lat(), request.lon(), false);
    }

    private void applyPatch(PlanStop stop, PatchStopRequest request) {
        if (request.has("label")) stop.setLabel(request.getLabel().trim());
        if (request.has("recipientName")) stop.setRecipientName(trimToNull(request.getRecipientName()));
        if (request.has("recipientPhone")) stop.setRecipientPhone(toE164(request.getRecipientPhone()));
        if (request.has("demands")) setDemands(stop, request.getDemands());
        if (request.has("serviceTimeSeconds"))
            stop.setServiceTimeSeconds(request.getServiceTimeSeconds() == null ? 0 : request.getServiceTimeSeconds());
        if (request.has("timeWindow")) setTimeWindow(stop, request.getTimeWindow());
        if (request.has("note")) stop.setNote(trimToNull(request.getNote()));

        String address = request.has("address") ? trimToNull(request.getAddress()) : stop.getAddressText();
        if (request.has("lat")) {
            // Coordinates sent: new coordinates, or null to drop them and geocode the address again.
            applyLocation(stop, address, request.getLat(), request.getLon(), request.getLat() == null);
        } else if (Objects.equals(address, stop.getAddressText())) {
            // Location untouched: keep coordinates and geocoding result.
        } else if (address != null) {
            // Only the address changed: geocode it again.
            applyLocation(stop, address, null, null, false);
        } else {
            // Address removed: the current coordinates become the only location.
            applyLocation(stop, null, stop.getLat(), stop.getLon(), false);
        }
    }

    /**
     * Sets the location of a stop and its geocoding state (D-18). A stop that already has this address and these
     * coordinates keeps its geocoding result (FR-P03): with coordinates, the same ones; without coordinates, the same
     * address and coordinates that did not come from the client. Otherwise coordinates make it {@code PROVIDED}, and
     * an address alone makes it {@code PENDING}.
     *
     * @param regeocode treat the location as changed even if it looks the same
     * @throws DetailedAppException {@code VALIDATION_ERROR} with {@code ADDRESS_OR_COORDINATES_REQUIRED} if the stop
     *                              would have neither (BR-08)
     */
    private static void applyLocation(PlanStop stop, String address, Double lat, Double lon, boolean regeocode) {
        if (address == null && lat == null)
            throw new DetailedAppException(ErrorCode.VALIDATION_ERROR,
                    List.of(new FieldErrorDetail("address", "ADDRESS_OR_COORDINATES_REQUIRED")));
        boolean known = stop.getId() != null && stop.getGeocodeStatus() != null && !regeocode;
        boolean sameAddress = Objects.equals(address, stop.getAddressText());
        boolean unchanged = lat != null
                ? known && sameAddress && lat.equals(stop.getLat()) && lon.equals(stop.getLon())
                : known && sameAddress && stop.getGeocodeStatus() != GeocodeStatus.PROVIDED;
        if (unchanged) return;
        stop.setAddressText(address);
        stop.setLat(lat);
        stop.setLon(lon);
        stop.setGeocodeStatus(lat != null ? GeocodeStatus.PROVIDED : GeocodeStatus.PENDING);
        stop.setGeocodeError(null);
        stop.setGeocodedDisplayName(null);
    }

    private static void setDemands(PlanStop stop, Map<CapacityUnit, Integer> demands) {
        stop.getDemands().clear();
        if (demands == null) return;
        // No row for a zero demand (DATA_MODEL §2.8a).
        demands.forEach((unit, quantity) -> {
            if (quantity > 0) stop.getDemands().put(unit, quantity);
        });
    }

    private static void setTimeWindow(PlanStop stop, TimeWindowRequest timeWindow) {
        stop.setTimeWindowStart(timeWindow == null ? null : timeWindow.start());
        stop.setTimeWindowEnd(timeWindow == null ? null : timeWindow.end());
    }

    /**
     * Every change to the draft invalidates the optimization result (FR-P03); the stops become unassigned.
     */
    private static void clearOptimization(RoutePlan plan) {
        plan.setOptimizedAt(null);
        plan.setTotalDistanceMeters(null);
        plan.setTotalDurationSeconds(null);
        for (PlanStop stop : plan.getStops()) {
            stop.setRoute(null);
            stop.setSequence(null);
            stop.setPlannedArrival(null);
        }
        for (PlanRoute route : plan.getRoutes()) {
            route.setPlannedDistanceMeters(null);
            route.setPlannedDurationSeconds(null);
            route.setPlannedStart(null);
            route.setPlannedEnd(null);
        }
    }

    /**
     * @return true if some stop has only an address and is waiting for geocoding; the plan is then GEOCODING
     */
    private static boolean markGeocodingIfPending(RoutePlan plan) {
        boolean pending = plan.getStops().stream().anyMatch(stop -> stop.getGeocodeStatus() == GeocodeStatus.PENDING);
        if (pending) plan.setProcessingStatus(ProcessingStatus.GEOCODING);
        return pending;
    }

    /**
     * Records the job in this transaction; it is submitted after the commit (D-19).
     */
    private void attachJob(RoutePlan plan, UserIdentity actor, JobType type) {
        plan.setActiveJobId(jobService.createJob(plan.getStoreId(), type, plan.getId(), actor.id()));
    }

    /**
     * Makes the plan row dirty so that {@code version} is incremented once, also when only its routes or stops changed
     * (they are excluded from the plan's optimistic lock).
     */
    private static void markModified(RoutePlan plan) {
        plan.setUpdatedAt(Instant.now());
    }

    private String toE164(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return null;
        // @ValidPhoneNumber has accepted the number already.
        return phoneNumberProcessor.toE164(phoneNumber);
    }

    private static Map<Long, Long> toCounts(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * @throws AppException {@code NOT_FOUND} for an actor without a store (ADMIN)
     */
    private static long storeIdOf(UserIdentity actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        return actor.storeId();
    }
}
