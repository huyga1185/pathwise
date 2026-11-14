package uk.huy.pathwise.audit.service;

import uk.huy.pathwise.audit.dto.AuditEntry;

/**
 * Public API of the {@code audit} module for writing (T58, P-61). Call it inside the transaction of the change, so
 * the event and the change are committed or rolled back together; without a transaction it opens its own.
 */
public interface AuditLog {
    /**
     * Records one event, with the time, the request id and the client IP. Details that are too large are cut: the
     * action itself never fails because of them.
     */
    void record(AuditEntry entry);
}
