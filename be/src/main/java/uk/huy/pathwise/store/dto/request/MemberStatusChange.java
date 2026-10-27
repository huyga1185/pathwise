package uk.huy.pathwise.store.dto.request;

import uk.huy.pathwise.shared.identity.UserStatus;

/**
 * Statuses an OWNER can set on a member (FR-S04).
 */
public enum MemberStatusChange {
    ACTIVE, DISABLED;

    public UserStatus toUserStatus() {
        return UserStatus.valueOf(name());
    }
}
