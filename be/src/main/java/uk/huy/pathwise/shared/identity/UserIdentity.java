package uk.huy.pathwise.shared.identity;

/**
 * Who the user is, as carried by the access token.
 *
 * @param storeId  store of the user (D-02); null only for {@code ADMIN}. Store-scoped queries must use this value,
 *                 never one from the request (BR-01).
 * @param canDrive the stored {@code can_drive} flag; use {@link #assignableToRoutes()} to decide who may drive
 */
public record UserIdentity(long id, String email, UserRole role, Long storeId, boolean canDrive) {
    /**
     * The single rule for "can be assigned routes and use the driver API" (D-21): a DRIVER always, an OWNER or
     * DISPATCHER only with the {@code canDrive} flag. ADMIN has no store and never drives.
     */
    public static boolean canBeAssignedRoutes(UserRole role, boolean canDrive) {
        return role == UserRole.DRIVER || (canDrive && role != UserRole.ADMIN);
    }

    public boolean assignableToRoutes() {
        return canBeAssignedRoutes(role, canDrive);
    }
}
