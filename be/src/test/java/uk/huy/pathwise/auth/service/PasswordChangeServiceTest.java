package uk.huy.pathwise.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import uk.huy.pathwise.auth.dto.request.ChangePasswordRequest;
import uk.huy.pathwise.auth.dto.request.RequestPasswordChangeOtpRequest;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.user.service.UserAccountService;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordChangeServiceTest {
    @Mock
    private UserAccountService userService;
    @Mock
    private OtpManager otpManager;
    @Mock
    private RefreshTokenManager refreshTokenManager;
    @Mock
    private PlatformTransactionManager transactionManager;

    private final AuthenticatedUser actor = new AuthenticatedUser(
            new UserIdentity(7L, "a@pathwise.test", UserRole.OWNER, 1L, false), "session-7");

    private PasswordChangeService service() {
        return new PasswordChangeService(userService, otpManager, refreshTokenManager, transactionManager);
    }

    @Test
    void requestChangeOtp_whenCurrentPasswordWrong_throwInvalidCurrentPasswordAndSendNothing() {
        when(userService.matchesPassword(7L, "wrong")).thenReturn(false);

        assertThatThrownBy(() -> service().requestChangeOtp(actor, new RequestPasswordChangeOtpRequest("wrong")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_CURRENT_PASSWORD);
        verifyNoInteractions(otpManager);
    }

    @Test
    void requestChangeOtp_whenCooldownRunning_throwOtpCooldown() {
        when(userService.matchesPassword(7L, "Passw0rd!x")).thenReturn(true);
        when(otpManager.sendOtp(OtpPurpose.CHANGE_PASSWORD, 7L, "a@pathwise.test")).thenReturn(OtpSendResult.COOLDOWN);

        assertThatThrownBy(() -> service().requestChangeOtp(actor, new RequestPasswordChangeOtpRequest("Passw0rd!x")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OTP_COOLDOWN);
    }

    @Test
    void changePassword_whenOtpWrong_throwInvalidOtpAndChangeNothing() {
        when(otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, 7L, "000000")).thenReturn(false);

        assertThatThrownBy(() -> service().changePassword(actor, new ChangePasswordRequest("000000", "N3wPassw0rd!")))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_OTP);
        verify(userService, never()).updateUserPassword(anyLong(), anyString());
        verify(refreshTokenManager, never()).revokeAllForUser(anyLong(), any());
    }
}
