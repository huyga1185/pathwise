package uk.huy.pathwise.store.dto.response;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditTargetType;
import uk.huy.pathwise.shared.identity.UserRole;

import java.time.Instant;
import java.util.Map;

/**
 * An audit event of the OWNER's store (T58, {@code GET /store/audit}).
 *
 * @param ip the client IP; null for an action of a platform ADMIN (P-59), and outside an HTTP request
 */
public record StoreAuditEventResponse(long id,
                                      Instant occurredAt,
                                      @Nullable Long actorId,
                                      @Nullable String actorEmail,
                                      @Nullable UserRole actorRole,
                                      AuditAction action,
                                      AuditTargetType targetType,
                                      @Nullable Long targetId,
                                      Map<String, Object> details,
                                      @Nullable String requestId,
                                      @Nullable String ip) {}
