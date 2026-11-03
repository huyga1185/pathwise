package uk.huy.pathwise.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.auth.dto.request.LogInRequest;
import uk.huy.pathwise.auth.infrastructure.ratelimit.AuthRateLimitPolicy;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.ratelimit.RateLimitDecision;
import uk.huy.pathwise.core.ratelimit.RateLimitedException;
import uk.huy.pathwise.core.statestore.StateStoreException;
import uk.huy.pathwise.user.dto.AuthenticationResult;
import uk.huy.pathwise.user.service.UserAccountService;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserAccountService userService;
    @Mock
    private AccessTokenManager accessTokenManager;
    @Mock
    private RefreshTokenManager refreshTokenManager;
    @Mock
    private AuthRateLimitPolicy rateLimitPolicy;

    @InjectMocks
    private AuthService authService;

    private final LogInRequest request = new LogInRequest("a@pathwise.test", "Passw0rd!x");
    private final ClientDetail client = new ClientDetail("JUnit", "127.0.0.1");

    @BeforeEach
    void allowByDefault() {
        lenient().when(rateLimitPolicy.logIn("127.0.0.1", "a@pathwise.test")).thenReturn(new RateLimitDecision.Allowed());
    }

    @Test
    void logIn_whenRateLimited_throwRateLimitedBeforeCheckingPassword() {
        when(rateLimitPolicy.logIn("127.0.0.1", "a@pathwise.test"))
                .thenReturn(new RateLimitDecision.Limited(Duration.ofSeconds(90)));

        assertThatThrownBy(() -> authService.logIn(client, request))
                .isInstanceOfSatisfying(RateLimitedException.class,
                        e -> assertThat(e.getRetryAfter()).isEqualTo(Duration.ofSeconds(90)));
        verifyNoInteractions(userService);
    }

    @Test
    void logIn_whenRateLimitCannotBeChecked_throwServiceUnavailable() {
        when(rateLimitPolicy.logIn("127.0.0.1", "a@pathwise.test")).thenThrow(new StateStoreException("Redis down"));

        assertThatThrownBy(() -> authService.logIn(client, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.SERVICE_UNAVAILABLE);
        verifyNoInteractions(userService);
    }

    @Test
    void logIn_whenInvalidCredentials_throwInvalidEmailOrPasswordAndIssueNoToken() {
        assertRejected(new AuthenticationResult.InvalidCredentials(), ErrorCode.INVALID_EMAIL_OR_PASSWORD);
    }

    @Test
    void logIn_whenEmailNotVerified_throwEmailNotVerifiedAndIssueNoToken() {
        assertRejected(new AuthenticationResult.EmailNotVerified(), ErrorCode.EMAIL_NOT_VERIFIED);
    }

    @Test
    void logIn_whenDisabled_throwAccountDisabledAndIssueNoToken() {
        assertRejected(new AuthenticationResult.Disabled(), ErrorCode.ACCOUNT_DISABLED);
    }

    private void assertRejected(AuthenticationResult result, ErrorCode expected) {
        when(userService.authenticate("a@pathwise.test", "Passw0rd!x")).thenReturn(result);

        assertThatThrownBy(() -> authService.logIn(client, request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(expected);

        verify(refreshTokenManager, never()).generateToken(anyLong(), any());
        verifyNoInteractions(accessTokenManager);
    }
}
