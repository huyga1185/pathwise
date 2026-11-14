package uk.huy.pathwise.admin.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.admin.dto.response.BusinessMetricsResponse;
import uk.huy.pathwise.admin.dto.response.TechnicalMetricsResponse;
import uk.huy.pathwise.admin.infrastructure.DatabaseStatsReader;
import uk.huy.pathwise.admin.infrastructure.MetricsCacheStore;
import uk.huy.pathwise.core.exception.DetailedAppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.FieldErrorDetail;
import uk.huy.pathwise.core.observability.FailureSource;
import uk.huy.pathwise.core.observability.OpsMetrics;
import uk.huy.pathwise.core.statestore.StateStoreException;
import uk.huy.pathwise.job.dto.DailyOptimizations;
import uk.huy.pathwise.job.service.JobStatsQuery;
import uk.huy.pathwise.planning.dto.DailyPlanStats;
import uk.huy.pathwise.planning.service.PlanStatsQuery;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.store.service.StoreDirectoryQuery;
import uk.huy.pathwise.user.service.UserDirectoryQuery;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The numbers of the ADMIN overview (T60, FR-A16, P-64). Business numbers come from the database, by day in
 * {@link #ZONE}, cached for {@link #CACHE_TTL}; technical numbers are those of this instance ({@link OpsMetrics}) plus
 * the database. Only totals: nothing of a store's content.
 */
@Service
@RequiredArgsConstructor
public class AdminMetricsService {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final int MAX_DAYS = 92;
    static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final int DEFAULT_DAYS = 30;

    private final StoreDirectoryQuery stores;
    private final UserDirectoryQuery users;
    private final PlanStatsQuery plans;
    private final JobStatsQuery jobs;
    private final MetricsCacheStore cache;
    private final ObjectMapper objectMapper;
    private final OpsMetrics opsMetrics;
    private final DatabaseStatsReader databaseStats;

    /**
     * @param from first day, inclusive; default: 29 days before {@code to}
     * @param to   last day, inclusive; default: today
     * @throws uk.huy.pathwise.core.exception.AppException {@code VALIDATION_ERROR} on {@code from}:
     *         {@code INVALID_DATE_RANGE} (after {@code to}), {@code DATE_RANGE_TOO_LONG} (more than 92 days)
     */
    public BusinessMetricsResponse business(@Nullable LocalDate from, @Nullable LocalDate to) {
        LocalDate last = to == null ? LocalDate.now(ZONE) : to;
        LocalDate first = from == null ? last.minusDays(DEFAULT_DAYS - 1) : from;
        if (first.isAfter(last)) throw invalid("INVALID_DATE_RANGE");
        if (ChronoUnit.DAYS.between(first, last) + 1 > MAX_DAYS) throw invalid("DATE_RANGE_TOO_LONG");

        String key = "metrics:business:" + first + ":" + last;
        String cached = readCache(key);
        if (cached != null) return objectMapper.readValue(cached, BusinessMetricsResponse.class);
        BusinessMetricsResponse response = compute(first, last);
        writeCache(key, objectMapper.writeValueAsString(response));
        return response;
    }

    private BusinessMetricsResponse compute(LocalDate first, LocalDate last) {
        Instant now = Instant.now();
        Instant from = first.atStartOfDay(ZONE).toInstant();
        Instant to = last.plusDays(1).atStartOfDay(ZONE).toInstant();
        ZoneOffset offset = ZONE.getRules().getOffset(now);

        Map<LocalDate, Long> newStores = stores.countCreatedByDay(from, to, offset);
        Map<LocalDate, DailyPlanStats> planDays = plans.dailyStats(from, to, offset);
        Map<LocalDate, DailyOptimizations> optimizationDays = jobs.optimizationsByDay(from, to, offset);
        List<BusinessMetricsResponse.Day> days = new ArrayList<>();
        long[] sum = new long[9];
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            DailyPlanStats p = planDays.getOrDefault(day, DailyPlanStats.NONE);
            DailyOptimizations o = optimizationDays.getOrDefault(day, new DailyOptimizations(0, 0));
            long[] values = {newStores.getOrDefault(day, 0L), p.plansCreated(), p.plansPublished(), p.plansCompleted(),
                    p.stopsDelivered(), p.stopsFailed(), p.stopsSkipped(), o.total(), o.failed()};
            for (int i = 0; i < values.length; i++) sum[i] += values[i];
            days.add(new BusinessMetricsResponse.Day(day, values[0], values[1], values[2], values[3], values[4],
                    values[5], values[6], values[7], values[8]));
        }
        long reported = sum[4] + sum[5] + sum[6];
        BusinessMetricsResponse.Totals totals = new BusinessMetricsResponse.Totals(sum[0], sum[1], sum[2], sum[3],
                sum[4], sum[5], sum[6], rate(sum[4], reported), rate(sum[5], reported), rate(sum[6], reported),
                sum[7], sum[8], rate(sum[8], sum[7]));

        StoreDirectoryQuery.StoreTotals storeTotals = stores.totals();
        Map<UserRole, Long> roles = users.countByRole();
        BusinessMetricsResponse.Now current = new BusinessMetricsResponse.Now(storeTotals.stores(),
                plans.countStoresPublishingSince(now.minus(Duration.ofDays(7))),
                plans.countStoresPublishingSince(now.minus(Duration.ofDays(30))), storeTotals.readOnly(),
                roles.get(UserRole.ADMIN), roles.get(UserRole.OWNER), roles.get(UserRole.DISPATCHER), roles.get(UserRole.DRIVER));
        return new BusinessMetricsResponse(first, last, ZONE.getId(), now, current, totals, days);
    }

    public TechnicalMetricsResponse technical() {
        Instant now = Instant.now();
        Duration hour = Duration.ofHours(1);
        Duration day = Duration.ofHours(24);
        long lastHour = opsMetrics.requests(hour);
        List<TechnicalMetricsResponse.Group> groups = opsMetrics.groups(hour).entrySet().stream()
                .map(entry -> new TechnicalMetricsResponse.Group(entry.getKey(), entry.getValue().requests(),
                        entry.getValue().p95Millis()))
                .toList();
        TechnicalMetricsResponse.Http http = new TechnicalMetricsResponse.Http(
                Math.round(opsMetrics.requests(Duration.ofMinutes(15)) / 15.0 * 100) / 100.0, lastHour,
                rate(opsMetrics.clientErrors(hour), lastHour), rate(opsMetrics.serverErrors(hour), lastHour), groups);
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        return new TechnicalMetricsResponse(opsMetrics.startedAt(),
                Duration.between(opsMetrics.startedAt(), now).toSeconds(), now, http,
                new TechnicalMetricsResponse.Jobs(jobs.countUnfinished(), jobs.countFailedSince(now.minus(day))),
                new TechnicalMetricsResponse.Failures(opsMetrics.failures(FailureSource.EMAIL, day),
                        opsMetrics.failures(FailureSource.ROUTING, day), opsMetrics.failures(FailureSource.GEOCODING, day),
                        opsMetrics.failures(FailureSource.JOB, day)),
                opsMetrics.rateLimited(day),
                new TechnicalMetricsResponse.Jvm(heap.getUsed(), heap.getMax() < 0 ? null : heap.getMax()),
                databaseStats.read());
    }

    private static @Nullable Double rate(long part, long whole) {
        return whole == 0 ? null : Math.round(part * 10000.0 / whole) / 10000.0;
    }

    private static DetailedAppException invalid(String code) {
        return new DetailedAppException(ErrorCode.VALIDATION_ERROR, List.of(new FieldErrorDetail("from", code)));
    }

    // The cache only saves time: Redis down means computing every time, not an error.
    private @Nullable String readCache(String key) {
        try {
            return cache.find(key).orElse(null);
        } catch (StateStoreException e) {
            return null;
        }
    }

    private void writeCache(String key, String json) {
        try {
            cache.save(key, json, CACHE_TTL);
        } catch (StateStoreException ignored) {
            // Next call computes again.
        }
    }
}
