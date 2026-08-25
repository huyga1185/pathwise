package uk.huy.pathwise.auth.token.refreshtoken;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.auth.infastructure.hash.SHA256Hasher;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.token.opaque.infrastructure.OpaqueTokenGenerator;
import uk.huy.pathwise.auth.token.opaque.infrastructure.SecureRandomOpaqueTokenGenerator;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenRotationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.model.RefreshToken;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.repository.RefreshTokenRepository;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.service.RefreshTokenService;

import java.time.Duration;
import java.util.List;
import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private final OpaqueTokenGenerator opaqueTokenGenerator = new SecureRandomOpaqueTokenGenerator();

    private SHA256Hasher sha256Hasher;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        this.sha256Hasher = new SHA256Hasher();

        this.refreshTokenService = new RefreshTokenService(refreshTokenRepository,
                Duration.ofDays(14),
                this.sha256Hasher,
                opaqueTokenGenerator);
    }

    @Test
    public void generateToken_whenAllArgumentsIsValid_returnRefreshTokenGenerationDetail() {
        RefreshTokenGenerationDetail detail = refreshTokenService.generateToken(1, new ClientDetail("UA-TEST", "0.0.0.0"));

        assertThat(detail).isNotNull();
        assertThat(detail.token()).isNotBlank();
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
        RefreshTokenGenerationDetail detail = refreshTokenService.generateToken(1L, null);

        assertThat(detail).isNotNull();
        assertThat(detail.token()).isNotBlank();
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
        RefreshTokenGenerationDetail detail = refreshTokenService.generateToken(1L, new ClientDetail(null, null));

        assertThat(detail).isNotNull();
        assertThat(detail.token()).isNotBlank();
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

        refreshTokenService.revokeToken(rawToken);

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
        refreshTokenService.revokeToken(null);

        verify(refreshTokenRepository, never()).findByTokenForUpdate(any());
        verify(refreshTokenRepository, never()).revokeFamily(any());
    }

    @Test
    public void revokeToken_whenRawTokenIsEmpty_return() {
        refreshTokenService.revokeToken("");

        verify(refreshTokenRepository, never()).findByTokenForUpdate(any());
        verify(refreshTokenRepository, never()).revokeFamily(any());
    }

    @Test
    public void revokeToken_whenRawTokenIsInvalid_return() {
        String rawToken = "ABCD";
        String hashedToken = this.sha256Hasher.hashString(rawToken);

        when(refreshTokenRepository.findByTokenForUpdate(hashedToken)).thenReturn(Optional.empty());

        refreshTokenService.revokeToken(rawToken);

        ArgumentCaptor<String> hashedTokenCapture = ArgumentCaptor.forClass(String.class);

        verify(refreshTokenRepository).findByTokenForUpdate(hashedTokenCapture.capture());
        verify(refreshTokenRepository, never()).revokeFamily(any());

        String hashedRFToken = hashedTokenCapture.getValue();

        assertThat(hashedRFToken).isEqualTo(hashedToken);
    }

    @Test
    public void rotateToken_whenRawTokenIsNull_returnEmpty() {
        Optional<RefreshTokenRotationDetail> result = refreshTokenService.rotateToken(null, null);

        assertThat(result).isEmpty();

        verify(refreshTokenRepository, never()).findByTokenForUpdate(any());
    }

    @Test
    public void rotateToken_whenRawTokenIsEmpty_returnEmpty() {
        Optional<RefreshTokenRotationDetail> result = refreshTokenService.rotateToken("", null);

        assertThat(result).isEmpty();

        verify(refreshTokenRepository, never()).findByTokenForUpdate(any());
    }

    @Test
    public void rotateToken_whenTokenNotFound_returnEmpty() {
        String rawToken = "ABCD";

        when(refreshTokenRepository.findByTokenForUpdate(any()))
                .thenReturn(Optional.empty());

        Optional<RefreshTokenRotationDetail> result =
                refreshTokenService.rotateToken(
                        rawToken,
                        new ClientDetail("UA", "IP")
                );

        assertThat(result).isEmpty();

        verify(refreshTokenRepository)
                .findByTokenForUpdate(sha256Hasher.hashString(rawToken));

        verify(refreshTokenRepository, never())
                .save(any());
    }
}
