package uk.huy.pathwise.audit.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.huy.pathwise.audit.dto.AuditFilter;
import uk.huy.pathwise.audit.dto.AuditRecord;

/**
 * Public API of the {@code audit} module for reading (T58). The caller scopes it: the OWNER endpoint always passes its
 * own store id.
 */
public interface AuditQuery {
    /**
     * @param pageable sort by {@code occurredAt} or {@code id}
     * @throws uk.huy.pathwise.core.exception.AppException {@code INVALID_SORT_FIELD} for another sort
     */
    Page<AuditRecord> search(AuditFilter filter, Pageable pageable);
}
