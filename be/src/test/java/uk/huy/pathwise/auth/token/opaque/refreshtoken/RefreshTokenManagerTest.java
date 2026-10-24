package uk.huy.pathwise.auth.token.opaque.refreshtoken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.auth.infrastructure.hash.Hasher;
import uk.huy.pathwise.auth.infrastructure.hash.SHA256Hasher;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.token.opaque.infrastructure.OpaqueTokenGenerator;
import uk.huy.pathwise.auth.token.opaque.infrastructure.SecureRandomOpaqueTokenGenerator;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.RejectReason;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rejected;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rotated;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshToken;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshTokenState;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.repository.RefreshTokenRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenManagerTest {
    private static final String RAW = "ABCD";
    private static final ClientDetail CLIENT = new ClientDetail("UA", "IP");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final OpaqueTokenGenerator opaqueTokenGenerator = new SecureRandomOpaqueTokenGenerator();

    private Hasher sha256Hasher;

    private RefreshTokenManager refreshTokenManager;

    @BeforeEach
    void setUp() {
        this.sha256Hasher = new SHA256Hasher();

        this.refreshTokenManager = new RefreshTokenManager(refreshTokenRepository,
                Duration.ofDays(14),
                this.sha256Hasher,
                opaqueTokenGenerator);
    }

    @Test
    public void generateToken_whenAllArgumentsIsValid_returnRefreshTokenGenerationDetail() {
        RefreshTokenGenerationDetail detail = refreshTokenManager.generateToken(1, new ClientDetail("UA-TEST", "0.0.0.0"));

        assertThat(detail).isNotNull();
        assertThat(detail.token()).isNotBlank();
        assertThat(detail.sessionId()).isNotBlank();
        assertThat(detail.expiresAt()).isNotNull();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken token = captor.getValue();

        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUserAgent()).isEqualTo("UA-TEST");
        assertThat(token.getIpAddress()).isEqualTo("0.0.0.0");
        assertThat(token.getUserId()).isEqualTo(1L);
        assertThat(token.getRotatedAt()).isNull();
        assertThat(token.getFamilyId()).isNotBlank();
        assertThat(token.getExpiresAt()).isNotNull();
    }

    @Test
    public void generateToken_whenClientDetailIsNull_returnRefreshTokenGenerationDetail() {
        RefreshTokenGenerationDetail detail = refreshTokenManager.generateToken(1L, null);

        assertThat(detail).isNotNull();
        assertThat(detail.token()).isNotBlank();
        assertThat(detail.sessionId()).isNotBlank();
        assertThat(detail.expiresAt()).isNotNull();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken token = captor.getValue();

        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUserAgent()).isNull();
        assertThat(token.getIpAddress()).isNull();
        assertThat(token.getUserId()).isEqualTo(1L);
        assertThat(token.getRotatedAt()).isNull();
        assertThat(token.getFamilyId()).isNotBlank();
        assertThat(token.getExpiresAt()).isNotNull();
    }

    @Test
    public void generateToken_whenClientDetailFieldsAreNull_returnRefreshTokenGenerator() {
        RefreshTokenGenerationDetail detail = refreshTokenManager.generateToken(1L, new ClientDetail(null, null));

        assertThat(detail).isNotNull();
        assertThat(detail.token()).isNotBlank();
        assertThat(detail.sessionId()).isNotBlank();
        assertThat(detail.expiresAt()).isNotNull();

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

        verify(refreshTokenRepository).save(captor.capture());

        RefreshToken token = captor.getValue();

        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getUserAgent()).isNull();
        assertThat(token.getIpAddress()).isNull();
        assertThat(token.getUserId()).isEqualTo(1L);
        assertThat(token.getRotatedAt()).isNull();
        assertThat(token.getFamilyId()).isNotBlank();
        assertThat(token.getExpiresAt()).isNotNull();
    }

    @Test
    public void revokeToken_whenRawTokenIsValid_revokeToken() {
        String rawToken = "ABCD";
        String hashedToken = this.sha256Hasher.hashString(rawToken);

        RefreshToken tempRt = new RefreshToken();
        tempRt.setFamilyId("ABCDE");

        when(refreshTokenRepository.findByTokenForUpdate(hashedToken)).thenReturn(Optional.of(tempRt));

        refreshTokenManager.revokeToken(rawToken);

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);

        verify(refreshTokenRepository).findByTokenForUpdate(argumentCaptor.capture());
        verify(refreshTokenRepository).revokeFamily(argumentCaptor.capture());

        List<String> list = argumentCaptor.getAllValues();

        String hashedRFToken = list.get(0);
        String familyId = list.get(1);

        assertThat(hashedRFToken).isEqualTo(hashedToken);
        assertThat(familyId).isEqualTo(tempRt.getFamilyId());
    }


    @Test
    public void revokeToken_whenRawTokenIsNull_return() {
        refreshTokenManager.revokeToken(null);

        verify(refreshTokenRepository, never()).findByTokenForUpdate(any());
        verify(refreshTokenRepository, never()).revokeFamily(any());
    }

    @Test
    public void revokeToken_whenRawTokenIsEmpty_return() {
        refreshTokenManager.revokeToken("");

        verify(refreshTokenRepository, never()).findByTokenForUpdate(any());
        verify(refreshTokenRepository, never()).revokeFamily(any());
    }

    @Test
    public void revokeToken_whenRawTokenIsInvalid_return() {
        String rawToken = "ABCD";
        String hashedToken = this.sha256Hasher.hashString(rawToken);

        when(refreshTokenRepository.findByTokenForUpdate(hashedToken)).thenReturn(Optional.empty());

        refreshTokenManager.revokeToken(rawToken);

        ArgumentCaptor<String> hashedTokenCapture = ArgumentCaptor.forClass(String.class);

        verify(refreshTokenRepository).findByTokenForUpdate(hashedTokenCapture.capture());
        verify(refreshTokenRepository, never()).revokeFamily(any());

        String hashedRFToken = hashedTokenCapture.getValue();

        assertThat(hashedRFToken).isEqualTo(hashedToken);
    }

    @Test
    public void rotateToken_whenRawTokenIsNull_rejectMissing() {
        assertThat(refreshTokenManager.rotateToken(null, CLIENT)).isEqualTo(new Rejected(RejectReason.MISSING));

        verify(refreshTokenRepository, never()).findByTokenForUpdate(any());
    }

    @Test
    public void rotateToken_whenRawTokenIsEmpty_rejectMissing() {
        assertThat(refreshTokenManager.rotateToken("", CLIENT)).isEqualTo(new Rejected(RejectReason.MISSING));

        verify(refreshTokenRepository, never()).findByTokenForUpdate(any());
    }

    @Test
    public void rotateToken_whenTokenNotFound_rejectNotFound() {
        when(refreshTokenRepository.findByTokenForUpdate(sha256Hasher.hashString(RAW))).thenReturn(Optional.empty());

        assertThat(refreshTokenManager.rotateToken(RAW, CLIENT)).isEqualTo(new Rejected(RejectReason.NOT_FOUND));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    public void rotateToken_whenActiveTokenValid_markUsedAndIssueChildInSameSession() {
        RefreshToken active = token(RefreshTokenState.ACTIVE, Instant.now().plus(Duration.ofDays(1)));
        found(active);

        RotationResult result = refreshTokenManager.rotateToken(RAW, CLIENT);

        assertThat(result).isInstanceOfSatisfying(Rotated.class, rotated -> {
            assertThat(rotated.sessionId()).isEqualTo("FAMILY");
            assertThat(rotated.userId()).isEqualTo(1L);
            assertThat(rotated.token()).isNotBlank();
        });
        assertThat(active.getState()).isEqualTo(RefreshTokenState.USED);
        assertThat(active.getRotatedAt()).isNotNull();
        verify(refreshTokenRepository, times(2)).save(any());
    }

    @Test
    public void rotateToken_whenActiveTokenExpired_markExpiredAndRejectExpired() {
        RefreshToken expired = token(RefreshTokenState.ACTIVE, Instant.now().minusSeconds(1));
        found(expired);

        assertThat(refreshTokenManager.rotateToken(RAW, CLIENT)).isEqualTo(new Rejected(RejectReason.EXPIRED));

        assertThat(expired.getState()).isEqualTo(RefreshTokenState.EXPIRED);
        verify(refreshTokenRepository, times(1)).save(expired);
    }

    @Test
    public void rotateToken_whenTokenAlreadyExpiredState_rejectExpired() {
        found(token(RefreshTokenState.EXPIRED, Instant.now().minusSeconds(1)));

        assertThat(refreshTokenManager.rotateToken(RAW, CLIENT)).isEqualTo(new Rejected(RejectReason.EXPIRED));
    }

    @Test
    public void rotateToken_whenTokenRevoked_rejectRevokedReused() {
        found(token(RefreshTokenState.REVOKED, Instant.now().plus(Duration.ofDays(1))));

        assertThat(refreshTokenManager.rotateToken(RAW, CLIENT)).isEqualTo(new Rejected(RejectReason.REVOKED_REUSED));

        verify(refreshTokenRepository, never()).revokeFamily(any());
    }

    @Test
    public void rotateToken_whenUsedTokenReusedAfterGrace_revokeFamilyAndRejectUsedReused() {
        RefreshToken used = token(RefreshTokenState.USED, Instant.now().plus(Duration.ofDays(1)));
        used.setRotatedAt(Instant.now().minusSeconds(60));
        found(used);

        assertThat(refreshTokenManager.rotateToken(RAW, CLIENT)).isEqualTo(new Rejected(RejectReason.USED_REUSED));

        verify(refreshTokenRepository).revokeFamily("FAMILY");
    }

    @Test
    public void rotateToken_whenUsedWithinGrace_rotateCurrentActiveKeepingItsExpiry() {
        RefreshToken used = token(RefreshTokenState.USED, Instant.now().plus(Duration.ofDays(1)));
        used.setRotatedAt(Instant.now());
        Instant activeExpiry = Instant.now().plus(Duration.ofDays(2));
        RefreshToken active = token(RefreshTokenState.ACTIVE, activeExpiry);
        found(used);
        when(refreshTokenRepository.findByFamilyIdAndState("FAMILY", RefreshTokenState.ACTIVE)).thenReturn(List.of(active));

        RotationResult result = refreshTokenManager.rotateToken(RAW, CLIENT);

        assertThat(result).isInstanceOfSatisfying(Rotated.class,
                rotated -> assertThat(rotated.expiresAt()).isEqualTo(activeExpiry));
        assertThat(active.getState()).isEqualTo(RefreshTokenState.USED);
    }

    @Test
    public void rotateToken_whenUsedWithinGraceButActiveExpired_markActiveExpiredAndRejectExpired() {
        RefreshToken used = token(RefreshTokenState.USED, Instant.now().plus(Duration.ofDays(1)));
        used.setRotatedAt(Instant.now());
        RefreshToken active = token(RefreshTokenState.ACTIVE, Instant.now().minusSeconds(1));
        found(used);
        when(refreshTokenRepository.findByFamilyIdAndState("FAMILY", RefreshTokenState.ACTIVE)).thenReturn(List.of(active));

        assertThat(refreshTokenManager.rotateToken(RAW, CLIENT)).isEqualTo(new Rejected(RejectReason.EXPIRED));

        assertThat(active.getState()).isEqualTo(RefreshTokenState.EXPIRED);
        verify(refreshTokenRepository, never()).revokeFamily(any());
    }

    @Test
    public void rotateToken_whenUsedWithinGraceButNoActiveToken_rejectInconsistentFamily() {
        RefreshToken used = token(RefreshTokenState.USED, Instant.now().plus(Duration.ofDays(1)));
        used.setRotatedAt(Instant.now());
        found(used);
        when(refreshTokenRepository.findByFamilyIdAndState("FAMILY", RefreshTokenState.ACTIVE)).thenReturn(List.of());

        assertThat(refreshTokenManager.rotateToken(RAW, CLIENT)).isEqualTo(new Rejected(RejectReason.INCONSISTENT_FAMILY));
    }

    @Test
    public void rotateToken_whenUsedTokenHasNoRotatedAt_rejectInconsistentFamily() {
        found(token(RefreshTokenState.USED, Instant.now().plus(Duration.ofDays(1))));

        assertThat(refreshTokenManager.rotateToken(RAW, CLIENT)).isEqualTo(new Rejected(RejectReason.INCONSISTENT_FAMILY));
    }

    @Test
    public void revokeSession_whenUserHasNoSuchActiveSession_returnFalseWithoutRevoking() {
        when(refreshTokenRepository.existsByUserIdAndFamilyIdAndStateAndExpiresAtAfter(eq(1L), eq("FAMILY"),
                eq(RefreshTokenState.ACTIVE), any())).thenReturn(false);

        assertThat(refreshTokenManager.revokeSession(1L, "FAMILY")).isFalse();

        verify(refreshTokenRepository, never()).revokeFamily(any());
    }

    @Test
    public void revokeSession_whenUserOwnsActiveSession_revokeFamily() {
        when(refreshTokenRepository.existsByUserIdAndFamilyIdAndStateAndExpiresAtAfter(eq(1L), eq("FAMILY"),
                eq(RefreshTokenState.ACTIVE), any())).thenReturn(true);

        assertThat(refreshTokenManager.revokeSession(1L, "FAMILY")).isTrue();

        verify(refreshTokenRepository).revokeFamily("FAMILY");
    }

    private void found(RefreshToken token) {
        when(refreshTokenRepository.findByTokenForUpdate(sha256Hasher.hashString(RAW))).thenReturn(Optional.of(token));
    }

    private static RefreshToken token(RefreshTokenState state, Instant expiresAt) {
        return RefreshToken.builder()
                .token("hash")
                .userId(1L)
                .familyId("FAMILY")
                .state(state)
                .expiresAt(expiresAt)
                .build();
    }
}
