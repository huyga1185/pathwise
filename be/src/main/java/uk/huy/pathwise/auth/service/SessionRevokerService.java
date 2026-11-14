package uk.huy.pathwise.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;

@Service
@RequiredArgsConstructor
public class SessionRevokerService implements SessionRevoker {
    private final RefreshTokenManager refreshTokenManager;

    @Override
    @Transactional
    public void revokeAllSessions(long userId) {
        refreshTokenManager.revokeAllForUser(userId, null);
    }
}
