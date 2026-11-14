package uk.huy.pathwise.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.audit.dto.AuditEntry;
import uk.huy.pathwise.audit.dto.AuditFilter;
import uk.huy.pathwise.audit.dto.AuditRecord;
import uk.huy.pathwise.audit.infrastructure.RequestOrigin;
import uk.huy.pathwise.audit.model.AuditEvent;
import uk.huy.pathwise.audit.repository.AuditEventRepository;
import uk.huy.pathwise.core.pagination.PaginationValidator;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService implements AuditLog, AuditQuery {
    static final int MAX_DETAILS = 2000;
    private static final Set<String> SORT_WHITELIST = Set.of("id", "occurredAt");
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() {};

    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void record(AuditEntry entry) {
        repository.save(AuditEvent.builder()
                .occurredAt(Instant.now())
                .actorId(entry.actor().id())
                .actorEmail(entry.actor().email())
                .actorRole(entry.actor().role())
                .storeId(entry.storeId())
                .action(entry.action())
                .targetType(entry.targetType())
                .targetId(entry.targetId())
                .details(details(entry.details()))
                .requestId(RequestOrigin.requestId())
                .ip(RequestOrigin.ip())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditRecord> search(AuditFilter filter, Pageable pageable) {
        PaginationValidator.validateSort(pageable, SORT_WHITELIST);
        return repository.search(filter.storeId(), filter.actorId(), filter.action(), filter.from(), filter.to(), pageable)
                .map(this::toRecord);
    }

    /**
     * The details as a JSON object of at most {@link #MAX_DETAILS} characters. A larger one becomes
     * {@code {"truncated": "<start of the JSON>"}}, so the action is recorded anyway.
     */
    String details(Map<String, ?> details) {
        if (details.isEmpty()) return null;
        String json = objectMapper.writeValueAsString(details);
        if (json.length() <= MAX_DETAILS) return json;
        log.warn("Audit details cut from {} characters", json.length());
        // Escaping can make the wrapper longer than the prefix: shorten until it fits.
        for (int keep = MAX_DETAILS - 100; ; keep -= 100) {
            String cut = objectMapper.writeValueAsString(Map.of("truncated", json.substring(0, Math.max(keep, 0))));
            if (cut.length() <= MAX_DETAILS) return cut;
        }
    }

    private AuditRecord toRecord(AuditEvent event) {
        return new AuditRecord(event.getId(), event.getOccurredAt(), event.getActorId(), event.getActorEmail(),
                event.getActorRole(), event.getStoreId(), event.getAction(), event.getTargetType(), event.getTargetId(),
                parse(event.getDetails()), event.getRequestId(), event.getIp());
    }

    private Map<String, Object> parse(String details) {
        if (details == null) return Map.of();
        try {
            return objectMapper.readValue(details, MAP);
        } catch (RuntimeException e) {
            return Map.of("raw", details);
        }
    }
}
