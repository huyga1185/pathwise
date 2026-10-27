package uk.huy.pathwise.store.dto.request;

import uk.huy.pathwise.shared.identity.UserRole;

/**
 * Roles a store member can be invited as or switched to; OWNER and ADMIN are never assigned through the API.
 */
public enum InvitableRole {
    DISPATCHER, DRIVER;

    public UserRole toUserRole() {
        return UserRole.valueOf(name());
    }
}
