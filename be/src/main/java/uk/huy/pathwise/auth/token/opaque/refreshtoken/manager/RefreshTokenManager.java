package uk.huy.pathwise.auth.token.opaque.refreshtoken.manager;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.auth.infrastructure.hash.Hasher;
import uk.huy.pathwise.auth.token.opaque.infrastructure.OpaqueTokenGenerator;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.RejectReason;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rejected;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rotated;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.SessionDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshToken;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshTokenState;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.repository.RefreshTokenRepository;
import uk.huy.pathwise.auth.model.ClientDetail;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
@Slf4j
public class RefreshTokenManager {
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration expirationTime;
    private final Hasher hasher;
    private final OpaqueTokenGenerator opaqueTokenGenerator;

    public RefreshTokenManager(RefreshTokenRepository refreshTokenRepository,
                               @Value("${token.expiration-time:14d}") Duration expirationTime,
                               Hasher hasher,
                               OpaqueTokenGenerator opaqueTokenGenerator) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationTime = expirationTime;
        this.hasher = hasher;
        this.opaqueTokenGenerator = opaqueTokenGenerator;
    }

    @Transactional
    public RefreshTokenGenerationDetail generateToken(long id, ClientDetail clientDetail) {
        String rawToken = opaqueTokenGenerator.generateOpaqueToken();
        String hashedToken = hasher.hashString(rawToken);
        RefreshToken refreshToken = RefreshToken.builder()
                .token(hashedToken)
                .userAgent(clientDetail != null && clientDetail.userAgent() != null ? clientDetail.userAgent() : null)
                .ipAddress(clientDetail != null && clientDetail.ip() != null ? clientDetail.ip() : null)
                .userId(id)
                .rotatedAt(null)
                .familyId(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plus(expirationTime))
                .build();
        refreshTokenRepository.save(refreshToken);
        return new RefreshTokenGenerationDetail(refreshToken.getFamilyId(), rawToken, refreshToken.getExpiresAt());
    }

    @Transactional
    public RotationResult rotateToken(String rawRefreshToken, ClientDetail detail) {
        if (rawRefreshToken == null || rawRefreshToken.isEmpty()) return new Rejected(RejectReason.MISSING);
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenForUpdate(hasher.hashString(rawRefreshToken));
        if (found.isEmpty()) return new Rejected(RejectReason.NOT_FOUND);

        RefreshToken token = found.get();
        return switch (token.getState()) {
            case ACTIVE -> markExpiredIfOverdue(token)
                    ? new Rejected(RejectReason.EXPIRED)
                    : rotate(token, detail, Instant.now().plus(expirationTime));
            case USED -> handleUsed(token, detail);
            case EXPIRED -> new Rejected(RejectReason.EXPIRED);
            case REVOKED -> {
                logReuse("Revoked refresh token reused", token, detail);
                yield new Rejected(RejectReason.REVOKED_REUSED);
            }
        };
    }

    private RotationResult handleUsed(RefreshToken token, ClientDetail detail) {
        if (token.getRotatedAt() == null) {
            log.error("Used refresh token does not have rotated at. tokenId={}", token.getId());
            return new Rejected(RejectReason.INCONSISTENT_FAMILY);
        }

        boolean withinGrace = token.getRotatedAt().plusSeconds(3).isAfter(Instant.now());

        if (!withinGrace) {
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            logReuse("Used refresh token reused", token, detail);
            return new Rejected(RejectReason.USED_REUSED);
        }

        List<RefreshToken> actives = refreshTokenRepository.findByFamilyIdAndState(token.getFamilyId(), RefreshTokenState.ACTIVE);

        if (actives.size() != 1) {
            log.error("Refresh token family within grace must have exactly one active token. familyId={}, actives={}",
                    token.getFamilyId(), actives.size());
            return new Rejected(RejectReason.INCONSISTENT_FAMILY);
        }

        RefreshToken active = actives.getFirst();
        if (markExpiredIfOverdue(active)) return new Rejected(RejectReason.EXPIRED);

        return rotate(active, detail, active.getExpiresAt());
    }

    /**
     * Marks an ACTIVE token whose lifetime is over as EXPIRED (the state is committed with the caller's transaction).
     */
    private boolean markExpiredIfOverdue(RefreshToken token) {
        if (token.getExpiresAt().isAfter(Instant.now())) return false;
        token.setState(RefreshTokenState.EXPIRED);
        refreshTokenRepository.save(token);
        return true;
    }

    private Rotated rotate(RefreshToken current, ClientDetail detail, Instant childExpiresAt) {
        current.setState(RefreshTokenState.USED);
        current.setRotatedAt(Instant.now());
        refreshTokenRepository.save(current);

        String newRawToken = opaqueTokenGenerator.generateOpaqueToken();
        RefreshToken child = RefreshToken.builder()
                .token(hasher.hashString(newRawToken))
                .userId(current.getUserId())
                .familyId(current.getFamilyId())
                .userAgent(detail.userAgent())
                .ipAddress(detail.ip())
                .rotatedAt(null)
                .expiresAt(childExpiresAt)
                .build();
        refreshTokenRepository.save(child);
        return new Rotated(child.getUserId(), child.getFamilyId(), newRawToken, child.getExpiresAt());
    }

    private void logReuse(String message,
                          @NonNull RefreshToken token,
                          @NonNull ClientDetail detail) {
        log.warn("{}. userId={}, familyId={}, issuedIp={}, issuedUA={}, reusedIp={}, reusedUA={}",
                message,
                token.getUserId(),
                token.getFamilyId(),
                token.getIpAddress(),
                token.getUserAgent(),
                detail.ip(),
                detail.userAgent());
    }

    @Transactional
    public void revokeToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) return;
        Optional<RefreshToken> token = refreshTokenRepository.findByTokenForUpdate(hasher.hashString(rawToken));
        if (token.isEmpty()) return;
        RefreshToken refreshToken = token.get();
        refreshTokenRepository.revokeFamily(refreshToken.getFamilyId());
    }

    /**
     * Active sessions of the user: one per family that still has an unexpired ACTIVE token.
     */
    @Transactional(readOnly = true)
    public List<SessionDetail> listSessions(long userId) {
        return refreshTokenRepository.findByUserIdAndStateAndExpiresAtAfter(userId, RefreshTokenState.ACTIVE, Instant.now())
                .stream()
                .map(token -> new SessionDetail(token.getFamilyId(),
                        token.getUserAgent(),
                        token.getIpAddress(),
                        token.getCreatedAt(),
                        token.getExpiresAt()))
                .toList();
    }

    /**
     * Revokes one active session (refresh token family) owned by {@code userId}. The current session of the caller
     * is not special here: callers decide whether it may be targeted.
     *
     * @param userId    owner the session must belong to
     * @param sessionId family to revoke
     * @return false if {@code userId} has no such active session (unknown, someone else's, revoked or expired)
     */
    @Transactional
    public boolean revokeSession(long userId, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        if (!refreshTokenRepository.existsByUserIdAndFamilyIdAndStateAndExpiresAtAfter(userId, sessionId,
                RefreshTokenState.ACTIVE, Instant.now())) return false;
        refreshTokenRepository.revokeFamily(sessionId);
        return true;
    }

    /**
     * Revokes every session of the user except {@code exceptSessionId} (null: all sessions). Reused by password
     * reset/change and member removal (T14, T15, T16).
     */
    @Transactional
    public void revokeAllForUser(long userId, String exceptSessionId) {
        refreshTokenRepository.revokeAllForUser(userId, exceptSessionId);
    }
}
