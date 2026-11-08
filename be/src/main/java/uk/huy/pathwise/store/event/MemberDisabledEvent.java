package uk.huy.pathwise.store.event;

/**
 * Published synchronously, inside the transaction that disables the member, so that listeners (the {@code auth}
 * module revokes every session, P-09) commit or roll back together with it (P-27).
 */
public record MemberDisabledEvent(long storeId, long userId) {}
