package uk.huy.pathwise.shared.identity;

/**
 * Account status. Only {@code ACTIVE} users can log in or refresh (BR-04).
 */
public enum UserStatus {
    PENDING_VERIFICATION, ACTIVE, DISABLED;
}
