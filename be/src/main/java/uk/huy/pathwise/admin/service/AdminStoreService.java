package uk.huy.pathwise.admin.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.admin.dto.response.AdminMemberResponse;
import uk.huy.pathwise.admin.dto.response.AdminStoreDetailResponse;
import uk.huy.pathwise.admin.dto.response.AdminStoreStateResponse;
import uk.huy.pathwise.admin.dto.response.TransferOwnerResponse;
import uk.huy.pathwise.auth.service.SessionRevoker;
import uk.huy.pathwise.store.dto.OwnerTransfer;
import uk.huy.pathwise.store.service.StoreOwnershipCommand;
import uk.huy.pathwise.admin.dto.response.AdminStoreSummaryResponse;
import uk.huy.pathwise.admin.dto.response.MemberCountsResponse;
import uk.huy.pathwise.admin.dto.response.StorePeriodStatsResponse;
import uk.huy.pathwise.audit.dto.AuditEntry;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditTargetType;
import uk.huy.pathwise.audit.service.AuditLog;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.fleet.service.VehicleQuery;
import uk.huy.pathwise.planning.dto.PlanActivity;
import uk.huy.pathwise.planning.dto.PlanPeriodStats;
import uk.huy.pathwise.planning.service.PlanStatsQuery;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.store.dto.StoreRecord;
import uk.huy.pathwise.shared.store.StoreStatus;
import uk.huy.pathwise.store.service.StoreDirectoryQuery;
import uk.huy.pathwise.store.service.StoreLockCommand;
import uk.huy.pathwise.user.service.UserDirectoryQuery;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The stores for the ADMIN (T55, FR-A11): one page of stores, then one aggregate query per module for the whole page.
 * Only totals and member accounts; never the content of a store (plans, vehicles, stops, recipients; P-59).
 */
@Service
@RequiredArgsConstructor
public class AdminStoreService {
    static final Duration RECENT = Duration.ofDays(30);
    private static final PlanActivity NO_PLAN = new PlanActivity(0, null);

    private final StoreDirectoryQuery storeDirectory;
    private final UserDirectoryQuery userDirectory;
    private final VehicleQuery vehicleQuery;
    private final PlanStatsQuery planStats;
    private final AuditLog auditLog;
    private final StoreLockCommand storeLock;
    private final StoreOwnershipCommand storeOwnership;
    private final SessionRevoker sessionRevoker;

    /**
     * @param text matched against the store name and the OWNER email; null or blank: every store
     * @throws AppException {@code INVALID_SORT_FIELD} outside id, name, createdAt
     */
    public PageResponse<AdminStoreSummaryResponse> listStores(@Nullable String text, @Nullable StoreStatus status,
                                                              Pageable pageable) {
        Set<Long> byOwner = text == null || text.isBlank() ? Set.of() : userDirectory.findStoreIdsByOwnerEmail(text);
        Page<StoreRecord> stores = storeDirectory.search(text, byOwner, status, pageable);
        List<Long> ids = stores.getContent().stream().map(StoreRecord::id).toList();
        Map<Long, String> owners = userDirectory.findOwnerEmails(ids);
        Map<Long, Map<UserRole, Long>> members = userDirectory.countMembersByRole(ids);
        Map<Long, Long> vehicles = vehicleQuery.countActiveByStore(ids);
        Map<Long, PlanActivity> activity = planStats.activityOf(ids);
        return PageResponse.from(stores.map(store -> {
            PlanActivity plans = activity.getOrDefault(store.id(), NO_PLAN);
            return new AdminStoreSummaryResponse(store.id(), store.name(), owners.get(store.id()), store.status(),
                    store.createdAt(), counts(members.get(store.id())), vehicles.getOrDefault(store.id(), 0L),
                    plans.plans(), plans.lastActivityAt());
        }));
    }

    /**
     * Records the view in the audit log (STORE_VIEWED, P-61): the OWNER can see when the platform looked at the store.
     *
     * @throws AppException {@code NOT_FOUND} if there is no such store
     */
    public AdminStoreDetailResponse getStore(UserIdentity admin, long storeId) {
        StoreRecord store = storeDirectory.find(storeId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        auditLog.record(AuditEntry.of(admin, storeId, AuditAction.STORE_VIEWED, AuditTargetType.STORE, storeId));
        List<Long> ids = List.of(storeId);
        PlanActivity plans = planStats.activityOf(ids).getOrDefault(storeId, NO_PLAN);
        Instant since = Instant.now().minus(RECENT);
        List<AdminMemberResponse> members = userDirectory.listMembers(storeId).stream()
                .map(member -> new AdminMemberResponse(member.id(), member.email(), member.role(), member.status(),
                        member.createdAt()))
                .toList();
        return new AdminStoreDetailResponse(store.id(), store.name(), userDirectory.findOwnerEmails(ids).get(storeId),
                store.status(), store.lockReason(), store.lockedAt(), store.timezone(), store.createdAt(),
                counts(userDirectory.countMembersByRole(ids).get(storeId)),
                vehicleQuery.countActiveByStore(ids).getOrDefault(storeId, 0L), plans.plans(), plans.lastActivityAt(),
                period(since, planStats.statsSince(storeId, since)), members);
    }

    /**
     * Locks the store READ_ONLY (T56, P-59): its members can still log in and read, drivers can still report results
     * and send their location; every other write gets {@code STORE_READ_ONLY}. Locking a locked store replaces the
     * reason. Recorded in the audit log.
     *
     * @param reason trimmed; shown to the members
     * @throws AppException {@code NOT_FOUND} if there is no such store
     */
    @Transactional
    public AdminStoreStateResponse lock(UserIdentity admin, long storeId, String reason) {
        String trimmed = reason.trim();
        StoreRecord store = storeLock.lock(storeId, admin.id(), trimmed);
        auditLog.record(new AuditEntry(admin, storeId, AuditAction.STORE_LOCKED, AuditTargetType.STORE, storeId,
                Map.of("reason", trimmed)));
        return state(store);
    }

    /**
     * Unlocks the store; an ACTIVE store is left as it is and nothing is recorded.
     *
     * @throws AppException {@code NOT_FOUND} if there is no such store
     */
    @Transactional
    public AdminStoreStateResponse unlock(UserIdentity admin, long storeId) {
        StoreRecord before = storeDirectory.find(storeId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (before.status() == StoreStatus.ACTIVE) return state(before);
        StoreRecord store = storeLock.unlock(storeId);
        auditLog.record(new AuditEntry(admin, storeId, AuditAction.STORE_UNLOCKED, AuditTargetType.STORE, storeId,
                before.lockReason() == null ? Map.of() : Map.of("reason", before.lockReason())));
        return state(store);
    }

    /**
     * Makes another member the OWNER (T57, FR-A13, P-63), for example when the OWNER lost their email. The previous
     * OWNER becomes a DISPATCHER and is logged out everywhere (their account may be compromised); a DRIVER who becomes
     * OWNER keeps driving. Both are emailed after the commit. Recorded in the audit log.
     *
     * @throws AppException {@code NOT_FOUND} for an unknown store or a user of another store; {@code VALIDATION_ERROR}
     *                      ({@code NEW_OWNER_NOT_ACTIVE}, {@code ALREADY_OWNER})
     */
    @Transactional
    public TransferOwnerResponse transferOwner(UserIdentity admin, long storeId, long userId) {
        // First: the store row is locked by the first read of the transaction (F-44).
        OwnerTransfer transfer = storeOwnership.transferOwner(storeId, userId);
        if (transfer.previousOwnerId() != null) sessionRevoker.revokeAllSessions(transfer.previousOwnerId());
        Map<String, Object> details = new java.util.LinkedHashMap<>();
        if (transfer.previousOwnerEmail() != null) details.put("from", transfer.previousOwnerEmail());
        details.put("to", transfer.newOwnerEmail());
        auditLog.record(new AuditEntry(admin, storeId, AuditAction.STORE_OWNER_TRANSFERRED, AuditTargetType.USER,
                transfer.newOwnerId(), details));
        return new TransferOwnerResponse(storeId, transfer.newOwnerId(), transfer.newOwnerEmail(),
                transfer.newOwnerCanDrive(), transfer.previousOwnerId(), transfer.previousOwnerEmail());
    }

    private static AdminStoreStateResponse state(StoreRecord store) {
        return new AdminStoreStateResponse(store.id(), store.status(), store.lockReason(), store.lockedAt());
    }

    private static MemberCountsResponse counts(@Nullable Map<UserRole, Long> byRole) {
        if (byRole == null) return new MemberCountsResponse(0, 0, 0);
        return new MemberCountsResponse(byRole.getOrDefault(UserRole.OWNER, 0L),
                byRole.getOrDefault(UserRole.DISPATCHER, 0L), byRole.getOrDefault(UserRole.DRIVER, 0L));
    }

    private static StorePeriodStatsResponse period(Instant since, PlanPeriodStats stats) {
        long reported = stats.stopsDelivered() + stats.stopsFailed() + stats.stopsSkipped();
        Double rate = reported == 0 ? null : Math.round(stats.stopsDelivered() * 10000.0 / reported) / 10000.0;
        return new StorePeriodStatsResponse(since, stats.plansCreated(), stats.plansPublished(), stats.plansCompleted(),
                stats.stopsDelivered(), stats.stopsFailed(), stats.stopsSkipped(), rate);
    }
}
