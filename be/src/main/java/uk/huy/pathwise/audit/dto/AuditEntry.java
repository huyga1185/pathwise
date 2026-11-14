package uk.huy.pathwise.audit.dto;

import org.jspecify.annotations.Nullable;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditTargetType;
import uk.huy.pathwise.shared.identity.UserIdentity;

import java.util.Map;

/**
 * What a module asks {@link uk.huy.pathwise.audit.service.AuditLog} to record. The time, request id and IP are added
 * by the audit module.
 *
 * @param actor   who did it
 * @param storeId the store concerned; null for an action outside any store
 * @param details small facts, no personal data except emails (P-59); cut when too large
 */
public record AuditEntry(UserIdentity actor,
                         @Nullable Long storeId,
                         AuditAction action,
                         AuditTargetType targetType,
                         @Nullable Long targetId,
                         Map<String, ?> details) {
    public static AuditEntry of(UserIdentity actor, @Nullable Long storeId, AuditAction action, AuditTargetType targetType,
                                @Nullable Long targetId) {
        return new AuditEntry(actor, storeId, action, targetType, targetId, Map.of());
    }
}
