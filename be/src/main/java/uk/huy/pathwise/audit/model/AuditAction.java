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
    STORE_VIEWED,
    /** The ADMIN locked a store READ_ONLY (T56); details: reason. */
    STORE_LOCKED,
    /** The ADMIN unlocked a store (T56); details: the reason of the lock. */
    STORE_UNLOCKED,
    /** The ADMIN disabled an account and ended its sessions (T56); details: email. */
    USER_DISABLED,
    /** The ADMIN enabled an account (T56); details: email, the new status. */
    USER_ENABLED,
    /** The ADMIN ended every session of an account (T56); details: email. */
    USER_SESSIONS_REVOKED
}
