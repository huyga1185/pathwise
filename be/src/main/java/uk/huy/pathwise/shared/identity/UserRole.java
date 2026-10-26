package uk.huy.pathwise.shared.identity;

/**
 * Platform role ({@code ADMIN}, no store) or role inside a store (D-03). One role per user (D-02).
 */
public enum UserRole {
    ADMIN, OWNER, DISPATCHER, DRIVER;
}
