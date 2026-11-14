package uk.huy.pathwise.audit.dto;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditTargetType;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;
import java.util.Map;

/**
 * An audit event as read back, for the ADMIN and OWNER endpoints.
 */
public record AuditRecord(long id,
                          Instant occurredAt,
                          @Nullable Long actorId,
                          @Nullable String actorEmail,
                          @Nullable UserRole actorRole,
                          @Nullable Long storeId,
                          AuditAction action,
                          AuditTargetType targetType,
                          @Nullable Long targetId,
                          Map<String, Object> details,
                          @Nullable String requestId,
                          @Nullable String ip) {}
