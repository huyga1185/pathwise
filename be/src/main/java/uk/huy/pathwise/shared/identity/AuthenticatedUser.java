package uk.huy.pathwise.shared.identity;

/**
 * Security principal of an authenticated request (P-25): who the user is and which session (refresh token
 * family, P-07) issued the access token. {@code sessionId} is null for access tokens issued before claim
 * {@code sid} existed (T11); they expire within 14 minutes.
 */
public record AuthenticatedUser(UserIdentity identity, String sessionId) {}
