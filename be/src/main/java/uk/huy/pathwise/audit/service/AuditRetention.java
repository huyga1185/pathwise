package uk.huy.pathwise.audit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.audit.repository.AuditEventRepository;
import uk.huy.pathwise.core.housekeeping.RetentionTask;

import java.time.Instant;

/**
 * Housekeeping of {@code audit_events} (T58, T53): events older than the cutoff (one year by default).
 */
@Component
@RequiredArgsConstructor
class AuditRetention implements RetentionTask {
    private final AuditEventRepository repository;

    @Override
    public String name() {
        return "audit-events";
    }

    @Override
    @Transactional
    public int purge(Instant cutoff) {
        return repository.deleteOccurredBefore(cutoff);
    }
}
