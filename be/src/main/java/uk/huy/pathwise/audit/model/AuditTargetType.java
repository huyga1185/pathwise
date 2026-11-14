package uk.huy.pathwise.audit.model;

/**
 * The kind of object an audit event is about; with {@code target_id}.
 */
public enum AuditTargetType {
    STORE,
    USER,
    INVITATION,
    PLAN
}
