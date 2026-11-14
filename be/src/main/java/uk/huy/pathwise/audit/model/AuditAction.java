package uk.huy.pathwise.audit.model;

/**
 * What an audit event records (T58, FR-A14). Only adds values: old rows keep their names.
 */
public enum AuditAction {
    // In a store, by its OWNER or DISPATCHER.
    /** Role, status or canDrive of a member changed; details: email and the changed fields (from, to). */
    MEMBER_UPDATED,
    /** A pending invitation revoked; details: email, role. */
    INVITATION_REVOKED,
    /** A plan published to the drivers. */
    PLAN_PUBLISHED,
    /** A plan cancelled; details: the status it had. */
    PLAN_CANCELLED,
    /** Routes of a plan changed by hand (P-58); details: number of violations after the edit. */
    PLAN_ROUTES_EDITED,

    // By a platform ADMIN.
    /** The ADMIN opened the detail of a store (GET /admin/stores/{id}). */
    STORE_VIEWED
}
