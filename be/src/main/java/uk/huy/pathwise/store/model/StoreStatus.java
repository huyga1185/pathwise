package uk.huy.pathwise.store.model;

/**
 * State of a store (T55, V7). READ_ONLY: locked by an ADMIN, members can read but not write (T56, P-59).
 */
public enum StoreStatus {
    ACTIVE,
    READ_ONLY
}
