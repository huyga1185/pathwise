package uk.huy.pathwise.auth.token.opaque.refreshtoken.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.repository.RefreshTokenRepository;
import uk.huy.pathwise.core.housekeeping.RetentionTask;

import java.time.Instant;

/**
 * Housekeeping of {@code refresh_tokens} (T53): tokens expired or revoked before the cutoff (30 days by default).
 */
@Component
@RequiredArgsConstructor
class RefreshTokenRetention implements RetentionTask {
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public String name() {
        return "refresh-tokens";
    }

    @Override
    @Transactional
    public int purge(Instant cutoff) {
        return refreshTokenRepository.deleteStale(cutoff);
    }
}
