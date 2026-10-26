package uk.huy.pathwise.shared.identity;

/**
 * Who the user is, as carried by the access token.
 *
 * @param storeId store of the user (D-02); null only for {@code ADMIN}. Store-scoped queries must use this value,
 *                never one from the request (BR-01).
 */
public record UserIdentity(long id, String email, UserRole role, Long storeId) {}
