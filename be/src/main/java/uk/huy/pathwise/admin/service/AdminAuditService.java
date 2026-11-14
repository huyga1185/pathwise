package uk.huy.pathwise.admin.service;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.admin.dto.response.AuditEventResponse;
import uk.huy.pathwise.audit.dto.AuditFilter;
import uk.huy.pathwise.audit.dto.AuditRecord;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.service.AuditQuery;
import uk.huy.pathwise.core.pagination.PageResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * The whole audit log, for the ADMIN (T58, FR-A14). Days are read in {@link #ZONE}, the platform's time zone.
 */
@Service
@RequiredArgsConstructor
public class AdminAuditService {
    static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AuditQuery auditQuery;

    /**
     * @param from first day, inclusive; null: no lower bound
     * @param to   last day, inclusive; null: no upper bound
     */
    public PageResponse<AuditEventResponse> listEvents(@Nullable Long storeId, @Nullable Long actorId,
                                                       @Nullable AuditAction action, @Nullable LocalDate from,
                                                       @Nullable LocalDate to, Pageable pageable) {
        AuditFilter filter = new AuditFilter(storeId, actorId, action, startOf(from),
                to == null ? null : startOf(to.plusDays(1)));
        return PageResponse.from(auditQuery.search(filter, pageable).map(AdminAuditService::toResponse));
    }

    private static @Nullable Instant startOf(@Nullable LocalDate day) {
        return day == null ? null : day.atStartOfDay(ZONE).toInstant();
    }

    private static AuditEventResponse toResponse(AuditRecord event) {
        return new AuditEventResponse(event.id(), event.occurredAt(), event.actorId(), event.actorEmail(),
                event.actorRole(), event.storeId(), event.action(), event.targetType(), event.targetId(),
                event.details(), event.requestId(), event.ip());
    }
}
