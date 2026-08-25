package uk.huy.pathwise.auth.token.opaque.refreshtoken.service;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.auth.infastructure.hash.SHA256Hasher;
import uk.huy.pathwise.auth.token.opaque.infrastructure.OpaqueTokenGenerator;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenRotationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshToken;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshTokenState;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.repository.RefreshTokenRepository;
import uk.huy.pathwise.auth.model.ClientDetail;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration expirationTime;
    private final SHA256Hasher sha256Hasher;
    private final OpaqueTokenGenerator opaqueTokenGenerator;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${token.expiration-time:14d}") Duration expirationTime,
                               SHA256Hasher sha256Hasher,
                               OpaqueTokenGenerator opaqueTokenGenerator) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationTime = expirationTime;
        this.sha256Hasher = sha256Hasher;
        this.opaqueTokenGenerator = opaqueTokenGenerator;
    }

    @Transactional
    public RefreshTokenGenerationDetail generateToken(long id, ClientDetail clientDetail) {
        String rawToken = opaqueTokenGenerator.generateOpaqueToken();
        String hashedToken = sha256Hasher.hashString(rawToken);
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
        return new RefreshTokenGenerationDetail(rawToken, refreshToken.getExpiresAt());
    }

    @Transactional
    public Optional<RefreshTokenRotationDetail> rotateToken(String rawRefreshToken, ClientDetail detail) {
        if (rawRefreshToken == null || rawRefreshToken.isEmpty()) return Optional.empty();
        String hashedToken = sha256Hasher.hashString(rawRefreshToken);
        Optional<RefreshToken> found = refreshTokenRepository.findByTokenForUpdate(hashedToken);
        if (found.isEmpty()) return Optional.empty();

        RefreshToken token = found.get();
        return switch (token.getState()) {
            case ACTIVE -> rotate(token, detail, Instant.now().plus(expirationTime));
            case USED -> handleUsed(token, detail);
            case EXPIRED -> Optional.empty();
            case REVOKED -> { logReuse("Revoked refresh token reused", token, detail); yield Optional.empty(); }
        };
    }

    private Optional<RefreshTokenRotationDetail> handleUsed(RefreshToken token, ClientDetail detail) {
        if (token == null) return Optional.empty();
        if (token.getRotatedAt() == null) {
            log.error("Used refresh token does not have rotated at. tokenId={}", token.getId());
            return Optional.empty();
        }

        boolean withinGrace = token.getRotatedAt().plusSeconds(3).isAfter(Instant.now());
        if (!withinGrace) {
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            logReuse("Used refresh token reused", token, detail);
            return Optional.empty();
        }

        List<RefreshToken> actives = refreshTokenRepository.findByFamilyIdAndState(token.getFamilyId(), RefreshTokenState.ACTIVE);
        if (actives.isEmpty()) {
            log.error("Non-revoked family has a USED token but no ACTIVE token. familyId={}", token.getFamilyId());
            return Optional.empty();
        }
        if (actives.size() > 1) {
            log.error("Refresh token family has more than one active token. familyId={}", token.getFamilyId());
            return Optional.empty();
        }
        RefreshToken active = actives.getFirst();
        return rotate(active, detail, active.getExpiresAt());
    }

    private Optional<RefreshTokenRotationDetail> rotate(RefreshToken current,
                                                        ClientDetail detail,
                                                        Instant childExpiresAt) {
        current.setState(RefreshTokenState.USED);
        current.setRotatedAt(Instant.now());
        refreshTokenRepository.save(current);

        String newRawToken = opaqueTokenGenerator.generateOpaqueToken();
        RefreshToken child = RefreshToken.builder()
                .token(sha256Hasher.hashString(newRawToken))
                .userId(current.getUserId())
                .familyId(current.getFamilyId())
                .userAgent(detail.userAgent())
                .ipAddress(detail.ip())
                .rotatedAt(null)
                .expiresAt(childExpiresAt)
                .build();
        refreshTokenRepository.save(child);
        return Optional.of(new RefreshTokenRotationDetail(child.getUserId(), newRawToken, child.getExpiresAt()));
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
        Optional<RefreshToken> token = refreshTokenRepository.findByTokenForUpdate(sha256Hasher.hashString(rawToken));
        if (token.isEmpty()) return;
        RefreshToken refreshToken = token.get();
        refreshTokenRepository.revokeFamily(refreshToken.getFamilyId());
    }

    public List<RefreshTokenDetail> getListInfoByUserId(long id) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndState(id, RefreshTokenState.ACTIVE);
        if (tokens.isEmpty()) return List.of();
        List<RefreshTokenDetail> infos = new ArrayList<>();
        for (RefreshToken token : tokens) {
            infos.add(new RefreshTokenDetail(token.getId(),
                    token.getUserAgent(),
                    token.getIpAddress(),
                    token.getExpiresAt(),
                    token.getCreatedAt()));
        }
        return infos;
    }
}
