package uk.huy.pathwise.auth.service;

/**
 * Public API of {@code auth} for the {@code admin} module (T56, P-62): ends every session of a user.
 */
public interface SessionRevoker {
    /**
     * Revokes every refresh token of the user, in the caller's transaction. Access tokens already issued stay valid
     * until they expire (at most 15 minutes, P-09).
     */
    void revokeAllSessions(long userId);
}
