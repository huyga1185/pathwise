package uk.huy.pathwise.store.model;

/**
 * Stored status of an invitation. A {@code PENDING} invitation past {@code expires_at} is reported as
 * {@code EXPIRED} without being rewritten (see {@link StoreInvitation#effectiveStatus}).
 */
public enum InvitationStatus {
    PENDING, ACCEPTED, REVOKED, EXPIRED;
}
