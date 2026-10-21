package uk.huy.pathwise.auth.token.opaque.otptoken;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.auth.infastructure.hash.Hasher;
import uk.huy.pathwise.auth.token.opaque.infrastructure.OpaqueTokenGenerator;
import uk.huy.pathwise.auth.token.opaque.otptoken.dto.OtpToken;
import uk.huy.pathwise.auth.token.opaque.otptoken.manager.OtpTokenManager;
import uk.huy.pathwise.auth.token.opaque.otptoken.statestore.OtpTokenStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpTokenManagerTest {
    @Mock
    private OtpTokenStore otpTokenStore;

    @Mock
    private Hasher hasher;

    @Mock
    private OpaqueTokenGenerator opaqueTokenGenerator;

    @InjectMocks
    private OtpTokenManager otpTokenManager;

    @Test
    void generateToken_returnOtpToken() {
        String purpose = "OK";

        when(opaqueTokenGenerator.generateOpaqueToken()).thenReturn("generated");
        when(hasher.hashString("generated")).thenReturn("hashed");

        Instant before = Instant.now();
        OtpToken token = otpTokenManager.generateToken(purpose, 42L);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> value = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);

        verify(otpTokenStore).save(key.capture(), value.capture(), ttl.capture());

        assertThat(key.getValue()).isEqualTo("otpToken:OK:generated");
        assertThat(value.getValue()).isEqualTo("42:hashed");
        assertThat(ttl.getValue()).isEqualTo(Duration.ofMinutes(5));

        assertThat(token.ticket()).isEqualTo("generated");
        assertThat(token.token()).isEqualTo("generated");
        assertThat(token.expiresAfter())
                .isBetween(before.plus(Duration.ofMinutes(5)).minusSeconds(5),
                        Instant.now().plus(Duration.ofMinutes(5)).plusSeconds(5));
    }

    @Test
    void verifyOtpToken_whenAllArgumentsValid_returnUserId() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("42:valid");
        when(hasher.match("valid", "otpToken")).thenReturn(true);

        Optional<Long> userId = otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose");

        assertThat(userId).contains(42L);
    }

    @Test
    void verifyOtpToken_whenHashDoesNotMatch_returnEmpty() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("42:valid");
        when(hasher.match("valid", "wrong")).thenReturn(false);

        Optional<Long> userId = otpTokenManager.verifyOtpToken("ticket", "wrong", "purpose");

        assertThat(userId).isEmpty();
    }

    @Test
    void verifyOtpToken_whenTokenIsNotExist_returnEmpty() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn(null);

        Optional<Long> userId = otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose");

        assertThat(userId).isEmpty();
    }

    @Test
    void verifyOtpToken_whenTokenIsEmpty_returnEmpty() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("");

        Optional<Long> userId = otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose");

        assertThat(userId).isEmpty();
    }

    @Test
    void verifyOtpToken_whenStoredValueHasNoUserId_returnEmpty() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("legacyhash");

        Optional<Long> userId = otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose");

        assertThat(userId).isEmpty();
        verifyNoInteractions(hasher);
    }
}
