package uk.huy.pathwise.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.huy.pathwise.auth.dto.request.UpdateUserPasswordRequest;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.token.jwt.manager.AccessTokenManager;
import uk.huy.pathwise.auth.token.opaque.otptoken.manager.OtpTokenManager;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.user.service.UserAccountService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    private static final String PURPOSE = "updateUserPassword";

    @Mock
    private UserAccountService userService;
    @Mock
    private AccessTokenManager accessTokenManager;
    @Mock
    private RefreshTokenManager refreshTokenManager;
    @Mock
    private OtpManager otpManager;
    @Mock
    private OtpTokenManager otpTokenManager;

    @InjectMocks
    private AuthService authService;

    private final UserIdentity userB = new UserIdentity(2L, "b@pathwise.test", UserRole.OWNER, 1L, false);
    private final UpdateUserPasswordRequest request = new UpdateUserPasswordRequest("newPassword123");

    @Test
    void updateUserPassword_whenTokenIssuedForAnotherUser_throwInvalidTokenAndKeepPassword() {
        when(otpTokenManager.verifyOtpToken("ticketA", "tokenA", PURPOSE)).thenReturn(Optional.of(1L));

        assertThatThrownBy(() -> authService.updateUserPassword(userB, "ticketA", "tokenA", request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);

        verify(userService, never()).updateUserPassword(anyLong(), anyString());
    }

    @Test
    void updateUserPassword_whenTokenInvalid_throwInvalidToken() {
        when(otpTokenManager.verifyOtpToken("ticket", "token", PURPOSE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.updateUserPassword(userB, "ticket", "token", request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_TOKEN);

        verify(userService, never()).updateUserPassword(anyLong(), anyString());
    }

    @Test
    void updateUserPassword_whenTokenIssuedForSameUser_updatePassword() {
        when(otpTokenManager.verifyOtpToken("ticketB", "tokenB", PURPOSE)).thenReturn(Optional.of(2L));

        authService.updateUserPassword(userB, "ticketB", "tokenB", request);

        verify(userService).updateUserPassword(2L, "newPassword123");
    }
}
