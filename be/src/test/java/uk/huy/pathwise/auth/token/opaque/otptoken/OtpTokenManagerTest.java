package uk.huy.pathwise.auth.token.opaque.otptoken;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.core.security.OpaqueTokenGenerator;
import uk.huy.pathwise.auth.token.opaque.otptoken.dto.OtpToken;
import uk.huy.pathwise.auth.token.opaque.otptoken.dto.OtpTokenVerification;
import uk.huy.pathwise.auth.token.opaque.otptoken.dto.OtpTokenVerification.RejectReason;
import uk.huy.pathwise.auth.token.opaque.otptoken.manager.OtpTokenManager;
import uk.huy.pathwise.auth.token.opaque.otptoken.statestore.OtpTokenStore;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        OtpToken token = otpTokenManager.generateToken(purpose, 42L, Duration.ofMinutes(5));

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
    void generateToken_whenTtlGiven_storeAndExpireWithThatTtl() {
        when(opaqueTokenGenerator.generateOpaqueToken()).thenReturn("generated");
        when(hasher.hashString("generated")).thenReturn("hashed");

        Instant before = Instant.now();
        OtpToken token = otpTokenManager.generateToken("RESET_PASSWORD", 42L, Duration.ofMinutes(10));

        verify(otpTokenStore).save("otpToken:RESET_PASSWORD:generated", "42:hashed", Duration.ofMinutes(10));
        assertThat(token.expiresAfter()).isAfterOrEqualTo(before.plus(Duration.ofMinutes(10)));
    }

    @Test
    void verifyOtpToken_whenAllArgumentsValid_returnVerifiedWithUserId() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("42:valid");
        when(hasher.match("valid", "otpToken")).thenReturn(true);

        assertThat(otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose"))
                .isEqualTo(new OtpTokenVerification.Verified(42L));
    }

    @Test
    void verifyOtpToken_whenHashDoesNotMatch_rejectMismatch() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("42:valid");
        when(hasher.match("valid", "wrong")).thenReturn(false);

        assertThat(otpTokenManager.verifyOtpToken("ticket", "wrong", "purpose"))
                .isEqualTo(new OtpTokenVerification.Rejected(RejectReason.MISMATCH));
    }

    @Test
    void verifyOtpToken_whenTokenIsNotExist_rejectNotFound() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn(null);

        assertThat(otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose"))
                .isEqualTo(new OtpTokenVerification.Rejected(RejectReason.NOT_FOUND));
    }

    @Test
    void verifyOtpToken_whenStoredValueIsEmpty_throwIllegalState() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("");

        assertThatThrownBy(() -> otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void verifyOtpToken_whenStoredValueHasNoUserId_throwIllegalStateWithoutCheckingHash() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("legacyhash");

        assertThatThrownBy(() -> otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose"))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(hasher);
    }

    @Test
    void verifyOtpToken_whenUserIdIsNotANumber_throwIllegalState() {
        when(otpTokenStore.getAndDelete("otpToken:purpose:ticket")).thenReturn("abc:hash");

        assertThatThrownBy(() -> otpTokenManager.verifyOtpToken("ticket", "otpToken", "purpose"))
                .isInstanceOf(IllegalStateException.class);
    }
}
