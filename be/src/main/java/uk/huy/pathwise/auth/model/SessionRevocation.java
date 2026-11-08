package uk.huy.pathwise.auth.model;

/**
 * What {@code DELETE /sessions/{sessionId}} ended up doing, so the controller can answer like {@code /log-out}
 * when the caller revoked the session it is using.
 */
public enum SessionRevocation {
    /** Another session of the caller was revoked; the caller stays logged in. */
    OTHER_SESSION_REVOKED,
    /** The caller's own current session was revoked: same effect as {@code /log-out}. */
    CURRENT_SESSION_LOGGED_OUT
}
