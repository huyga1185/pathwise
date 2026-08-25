package uk.huy.pathwise.auth.token.opaque.refreshtoken.model;
/**
 * Defines the possible states of a refresh token during its lifecycle.
 */
public enum RefreshTokenState {

    /**
     * Token is active and can be used.
     */
    ACTIVE,

    /**
     * Token has been consumed and cannot be reused.
     */
    USED,

    /**
     * Token has been explicitly revoked due to user logout, administrative action, or security concerns.
     */
    REVOKED,

    /**
     * Token has expired.
     */
    EXPIRED
}
