package uk.huy.pathwise.planning.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.DetailedAppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.FieldErrorDetail;
import uk.huy.pathwise.fleet.dto.FleetVehicle;
import uk.huy.pathwise.fleet.service.VehicleQuery;
import uk.huy.pathwise.planning.dto.response.StoreReportResponse;
import uk.huy.pathwise.planning.dto.response.StoreReportResponse.ReportDay;
import uk.huy.pathwise.planning.dto.response.StoreReportResponse.ReportDriver;
import uk.huy.pathwise.planning.dto.response.StoreReportResponse.ReportFailureReason;
import uk.huy.pathwise.planning.dto.response.StoreReportResponse.ReportPlanCounts;
import uk.huy.pathwise.planning.dto.response.StoreReportResponse.ReportStopCounts;
import uk.huy.pathwise.planning.model.*;
import uk.huy.pathwise.planning.repository.RoutePlanRepository;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.store.dto.StoreMember;
import uk.huy.pathwise.store.service.StoreMemberQuery;
import uk.huy.pathwise.store.service.StoreQuery;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reports of the caller's store by {@code planDate} (T63, P-67, P-68): a summary, and every stop as CSV. Read only; the
 * store comes from the token (BR-01).
 */
@Service
@RequiredArgsConstructor
public class StoreReportService {
    /** P-68. */
    static final int MAX_DAYS = 31;
    private static final int TOP_REASONS = 10;
    private static final Set<PlanStatus> GIVEN_TO_DRIVERS = EnumSet.of(PlanStatus.PUBLISHED, PlanStatus.IN_PROGRESS,
            PlanStatus.COMPLETED);
    private static final Set<PlanStatus> NOT_CANCELLED = EnumSet.complementOf(EnumSet.of(PlanStatus.CANCELLED));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final List<String> CSV_HEADER = List.of("Ngày", "Kế hoạch", "Trạng thái kế hoạch", "Tài xế", "Xe",
            "Thứ tự", "Nhãn / mã đơn", "Người nhận", "Số điện thoại", "Địa chỉ", "Giờ dự kiến", "Kết quả", "Lý do",
            "Giờ báo");
    private static final Map<PlanStatus, String> PLAN_LABELS = Map.of(PlanStatus.DRAFT, "Nháp",
            PlanStatus.PUBLISHED, "Đã giao việc", PlanStatus.IN_PROGRESS, "Đang giao", PlanStatus.COMPLETED, "Hoàn thành",
            PlanStatus.CANCELLED, "Đã huỷ");
    private static final Map<StopStatus, String> STOP_LABELS = Map.of(StopStatus.PENDING, "Chờ giao",
            StopStatus.DELIVERED, "Đã giao", StopStatus.FAILED, "Không giao được", StopStatus.SKIPPED, "Bỏ qua");

    private final RoutePlanRepository planRepository;
    private final StoreQuery storeQuery;
    private final StoreMemberQuery storeMemberQuery;
    private final VehicleQuery vehicleQuery;

    /**
     * The period of a report: today in the store timezone by default.
     *
     * @throws AppException {@code VALIDATION_ERROR} on {@code from}: {@code INVALID_DATE_RANGE} (after {@code to}),
     *                      {@code DATE_RANGE_TOO_LONG} (more than {@value #MAX_DAYS} days)
     */
    public Period period(UserIdentity actor, @Nullable LocalDate from, @Nullable LocalDate to) {
        long storeId = storeIdOf(actor);
        ZoneId zone = ZoneId.of(storeQuery.findStore(storeId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND)).timezone());
        LocalDate last = to != null ? to : from != null ? from : LocalDate.now(zone);
        LocalDate first = from != null ? from : last;
        if (first.isAfter(last)) throw invalid("INVALID_DATE_RANGE");
        if (ChronoUnit.DAYS.between(first, last) + 1 > MAX_DAYS) throw invalid("DATE_RANGE_TOO_LONG");
        return new Period(storeId, first, last);
    }

    @Transactional(readOnly = true)
    public StoreReportResponse summary(Period period) {
        Map<PlanStatus, Long> byStatus = new EnumMap<>(PlanStatus.class);
        for (Object[] row : planRepository.countByStatusBetween(period.storeId(), period.from(), period.to()))
            byStatus.put((PlanStatus) row[0], (Long) row[1]);
        ReportPlanCounts plans = new ReportPlanCounts(byStatus.values().stream().mapToLong(Long::longValue).sum(),
                byStatus.getOrDefault(PlanStatus.DRAFT, 0L), byStatus.getOrDefault(PlanStatus.PUBLISHED, 0L),
                byStatus.getOrDefault(PlanStatus.IN_PROGRESS, 0L), byStatus.getOrDefault(PlanStatus.COMPLETED, 0L),
                byStatus.getOrDefault(PlanStatus.CANCELLED, 0L));

        List<RoutePlan> given = planRepository.findInPeriod(period.storeId(), period.from(), period.to(), GIVEN_TO_DRIVERS);
        Map<LocalDate, long[]> days = new TreeMap<>();
        for (LocalDate day = period.from(); !day.isAfter(period.to()); day = day.plusDays(1)) days.put(day, new long[5]);
        long[] stops = new long[4];
        long distance = 0;
        Map<Long, DriverTally> drivers = new HashMap<>();
        Map<String, ReasonTally> reasons = new LinkedHashMap<>();
        for (RoutePlan plan : given) {
            long[] day = days.get(plan.getPlanDate());
            day[0]++;
            Set<Long> usedRoutes = new HashSet<>();
            for (PlanStop stop : plan.getStops()) {
                int index = stop.getStatus().ordinal();
                stops[index]++;
                day[1 + index]++;
                if (stop.getRoute() != null) {
                    usedRoutes.add(stop.getRoute().getId());
                    drivers.computeIfAbsent(stop.getRoute().getDriverId(), id -> new DriverTally()).add(stop);
                }
                if (stop.getStatus() == StopStatus.FAILED && stop.getFailureReason() != null && !stop.getFailureReason().isBlank()) {
                    String reason = stop.getFailureReason().strip();
                    reasons.computeIfAbsent(reason.toLowerCase(Locale.ROOT), key -> new ReasonTally(reason)).count++;
                }
            }
            for (PlanRoute route : plan.getRoutes()) {
                if (!usedRoutes.contains(route.getId())) continue;
                long planned = route.getPlannedDistanceMeters() == null ? 0 : route.getPlannedDistanceMeters();
                distance += planned;
                DriverTally tally = drivers.get(route.getDriverId());
                tally.routes++;
                tally.distance += planned;
            }
        }

        Map<Long, String> names = driverNames(period.storeId(), drivers.keySet());
        List<ReportDriver> driverRows = drivers.entrySet().stream()
                .map(entry -> entry.getValue().toDriver(entry.getKey(), names.getOrDefault(entry.getKey(), "")))
                .sorted(Comparator.comparing(ReportDriver::fullName).thenComparing(ReportDriver::driverId))
                .toList();
        List<ReportFailureReason> topReasons = reasons.values().stream()
                .sorted(Comparator.comparingLong((ReasonTally tally) -> -tally.count).thenComparing(tally -> tally.reason))
                .limit(TOP_REASONS)
                .map(tally -> new ReportFailureReason(tally.reason, tally.count))
                .toList();
        long done = stops[StopStatus.DELIVERED.ordinal()] + stops[StopStatus.FAILED.ordinal()] + stops[StopStatus.SKIPPED.ordinal()];
        ReportStopCounts stopCounts = new ReportStopCounts(Arrays.stream(stops).sum(), stops[StopStatus.DELIVERED.ordinal()],
                stops[StopStatus.FAILED.ordinal()], stops[StopStatus.SKIPPED.ordinal()], stops[StopStatus.PENDING.ordinal()],
                done == 0 ? null : (double) stops[StopStatus.DELIVERED.ordinal()] / done);
        List<ReportDay> dayRows = days.entrySet().stream()
                .map(entry -> {
                    long[] d = entry.getValue();
                    return new ReportDay(entry.getKey(), d[0], d[1 + StopStatus.DELIVERED.ordinal()], d[1 + StopStatus.FAILED.ordinal()],
                            d[1 + StopStatus.SKIPPED.ordinal()], d[1 + StopStatus.PENDING.ordinal()]);
                })
                .toList();
        return new StoreReportResponse(period.from(), period.to(), plans, stopCounts, distance, dayRows, driverRows, topReasons);
    }

    /**
     * Every stop of the plans of the period that are not cancelled, one CSV row each, written as it goes: UTF-8 with
     * a BOM for Excel, RFC 4180 quoting, and a {@code '} before a cell that a spreadsheet would run as a formula.
     * Times are local to each plan.
     */
    @Transactional(readOnly = true)
    public void writeStopsCsv(Period period, Writer out) {
        List<RoutePlan> plans = planRepository.findInPeriod(period.storeId(), period.from(), period.to(), NOT_CANCELLED);
        Set<Long> driverIds = new HashSet<>();
        Set<Long> vehicleIds = new HashSet<>();
        plans.forEach(plan -> plan.getRoutes().forEach(route -> {
            driverIds.add(route.getDriverId());
            vehicleIds.add(route.getVehicleId());
        }));
        Map<Long, String> drivers = driverNames(period.storeId(), driverIds);
        Map<Long, String> vehicles = vehicleIds.isEmpty() ? Map.of() : vehicleQuery.findAll(period.storeId(), vehicleIds).stream()
                .collect(Collectors.toMap(FleetVehicle::id, FleetVehicle::name));
        try {
            out.write('﻿');
            writeRow(out, CSV_HEADER);
            for (RoutePlan plan : plans) {
                ZoneId zone = ZoneId.of(plan.getTimezone());
                Map<Long, PlanRoute> routes = plan.getRoutes().stream().collect(Collectors.toMap(PlanRoute::getId, Function.identity()));
                List<PlanStop> stops = plan.getStops().stream()
                        .sorted(Comparator.comparing((PlanStop stop) -> stop.getRoute() == null ? Long.MAX_VALUE : stop.getRoute().getId())
                                .thenComparing(stop -> stop.getSequence() == null ? Integer.MAX_VALUE : stop.getSequence())
                                .thenComparing(PlanStop::getId))
                        .toList();
                for (PlanStop stop : stops) {
                    PlanRoute route = stop.getRoute() == null ? null : routes.get(stop.getRoute().getId());
                    writeRow(out, List.of(
                            plan.getPlanDate().format(DATE),
                            plan.getName(),
                            PLAN_LABELS.get(plan.getStatus()),
                            route == null ? "" : drivers.getOrDefault(route.getDriverId(), ""),
                            route == null ? "" : vehicles.getOrDefault(route.getVehicleId(), ""),
                            stop.getSequence() == null ? "" : String.valueOf(stop.getSequence()),
                            stop.getLabel(),
                            orEmpty(stop.getRecipientName()),
                            phone(stop.getRecipientPhone()),
                            orEmpty(stop.getAddressText()),
                            stop.getPlannedArrival() == null ? "" : stop.getPlannedArrival().atZone(zone).format(TIME),
                            STOP_LABELS.get(stop.getStatus()),
                            orEmpty(stop.getFailureReason()),
                            stop.getStatusUpdatedAt() == null ? "" : stop.getStatusUpdatedAt().atZone(zone).format(DATE_TIME)));
                }
            }
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * ReportDriver id to name; a member without a name maps to "".
     */
    private Map<Long, String> driverNames(long storeId, Set<Long> ids) {
        Map<Long, String> names = new HashMap<>();
        if (ids.isEmpty()) return names;
        for (StoreMember member : storeMemberQuery.findMembers(storeId, ids))
            names.put(member.id(), member.fullName() == null ? "" : member.fullName());
        return names;
    }

    private static void writeRow(Writer out, List<String> cells) throws IOException {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) out.write(',');
            out.write(csvCell(cells.get(i)));
        }
        out.write("\r\n");
    }

    /**
     * Quotes when needed; a cell starting with {@code = + - @}, tab or CR gets a leading {@code '} so that a
     * spreadsheet shows it as text instead of running it (CSV injection, P-68).
     */
    static String csvCell(String value) {
        String cell = value;
        if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) cell = "'" + cell;
        if (cell.contains(",") || cell.contains("\"") || cell.contains("\n") || cell.contains("\r"))
            cell = "\"" + cell.replace("\"", "\"\"") + "\"";
        return cell;
    }

    /**
     * {@code +84905123456} → {@code 0905 123 456}, like the clients show it; other numbers as stored.
     */
    static String phone(@Nullable String e164) {
        if (e164 == null) return "";
        if (e164.startsWith("+84") && e164.length() == 12) {
            String national = "0" + e164.substring(3);
            return national.substring(0, 4) + " " + national.substring(4, 7) + " " + national.substring(7);
        }
        return e164;
    }

    private static String orEmpty(@Nullable String value) {
        return value == null ? "" : value;
    }

    private static DetailedAppException invalid(String code) {
        return new DetailedAppException(ErrorCode.VALIDATION_ERROR, List.of(new FieldErrorDetail("from", code)));
    }

    private static long storeIdOf(UserIdentity actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        return actor.storeId();
    }

    /**
     * A checked period of the caller's store, both days included.
     */
    public record Period(long storeId, LocalDate from, LocalDate to) {}

    private static final class DriverTally {
        long routes;
        long distance;
        final long[] stops = new long[4];
        @Nullable Instant first;
        @Nullable Instant last;

        void add(PlanStop stop) {
            stops[stop.getStatus().ordinal()]++;
            Instant at = stop.getStatusUpdatedAt();
            if (at == null || stop.getStatus() == StopStatus.PENDING) return;
            if (first == null || at.isBefore(first)) first = at;
            if (last == null || at.isAfter(last)) last = at;
        }

        ReportDriver toDriver(long id, String name) {
            return new ReportDriver(id, name, routes, stops[StopStatus.DELIVERED.ordinal()], stops[StopStatus.FAILED.ordinal()],
                    stops[StopStatus.SKIPPED.ordinal()], stops[StopStatus.PENDING.ordinal()], distance, first, last);
        }
    }

    private static final class ReasonTally {
        final String reason;
        long count;

        ReasonTally(String reason) {
            this.reason = reason;
        }
    }
}
