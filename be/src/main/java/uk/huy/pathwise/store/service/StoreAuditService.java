package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.audit.dto.AuditFilter;
import uk.huy.pathwise.audit.dto.AuditRecord;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.service.AuditQuery;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.store.dto.response.StoreAuditEventResponse;
import uk.huy.pathwise.store.repository.StoreRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The audit log of the OWNER's own store (T58, FR-A14): the store id always comes from the token (BR-01). The IP of a
 * platform ADMIN is hidden (P-59).
 */
@Service
@RequiredArgsConstructor
public class StoreAuditService {
    private final AuditQuery auditQuery;
    private final StoreRepository storeRepository;

    /**
     * @param from first day, in the store's time zone; null: no lower bound
     * @param to   last day (inclusive), in the store's time zone; null: no upper bound
     * @throws AppException {@code INVALID_SORT_FIELD} outside occurredAt, id
     */
    public PageResponse<StoreAuditEventResponse> listEvents(UserIdentity actor, @Nullable Long actorId,
                                                            @Nullable AuditAction action, @Nullable LocalDate from,
                                                            @Nullable LocalDate to, Pageable pageable) {
        long storeId = StoreService.storeIdOf(actor);
        ZoneId zone = storeRepository.findById(storeId)
                .map(store -> ZoneId.of(store.getTimezone()))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        AuditFilter filter = new AuditFilter(storeId, actorId, action, startOf(from, zone),
                to == null ? null : startOf(to.plusDays(1), zone));
        return PageResponse.from(auditQuery.search(filter, pageable).map(StoreAuditService::toResponse));
    }

    private static @Nullable Instant startOf(@Nullable LocalDate day, ZoneId zone) {
        return day == null ? null : day.atStartOfDay(zone).toInstant();
    }

    private static StoreAuditEventResponse toResponse(AuditRecord event) {
        String ip = event.actorRole() == UserRole.ADMIN ? null : event.ip();
        return new StoreAuditEventResponse(event.id(), event.occurredAt(), event.actorId(), event.actorEmail(),
                event.actorRole(), event.action(), event.targetType(), event.targetId(), event.details(),
                event.requestId(), ip);
    }
}
