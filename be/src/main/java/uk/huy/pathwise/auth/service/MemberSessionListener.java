package uk.huy.pathwise.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.store.event.MemberDisabledEvent;

/**
 * Sessions follow the member's status (P-09, P-27). The {@code store} module cannot call {@code auth} (ARCHITECTURE.md
 * §2.1), so it publishes an event; this listener runs synchronously inside the publisher's transaction, so the member
 * is disabled and its sessions revoked together or not at all.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemberSessionListener {
    private final RefreshTokenManager refreshTokenManager;

    @EventListener
    public void onMemberDisabled(MemberDisabledEvent event) {
        refreshTokenManager.revokeAllForUser(event.userId(), null);
        log.info("Sessions revoked for disabled member. storeId={}, userId={}", event.storeId(), event.userId());
    }
}
