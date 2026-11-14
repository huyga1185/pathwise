package uk.huy.pathwise.admin.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditTargetType;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;
import java.util.Map;

/**
 * An audit event for the ADMIN (T58, {@code GET /admin/audit}).
 *
 * @param storeId   null for an action outside any store
 * @param requestId null outside an HTTP request
 * @param ip        null outside an HTTP request
 */
public record AuditEventResponse(long id,
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
