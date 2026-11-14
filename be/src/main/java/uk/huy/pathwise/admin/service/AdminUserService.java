package uk.huy.pathwise.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.audit.dto.AuditEntry;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditTargetType;
import uk.huy.pathwise.audit.service.AuditLog;
import uk.huy.pathwise.auth.service.AccountSupport;
import uk.huy.pathwise.auth.service.SessionRevoker;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.service.UserAccountService;
import uk.huy.pathwise.user.service.UserStatusCommand;

import java.util.Map;

/**
 * Account actions of the ADMIN (T56, FR-A12): disable, enable, end every session. Each is recorded in the audit log in
 * the same transaction, with the user's store so that its OWNER sees it.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserAccountService userAccounts;
    private final UserStatusCommand userStatus;
    private final SessionRevoker sessionRevoker;
    private final AuditLog auditLog;
    private final AccountSupport accountSupport;

    /**
     * Disables the account and revokes its sessions: it cannot refresh, and the next log-in gets
     * {@code ACCOUNT_DISABLED}. Access tokens already issued expire within 15 minutes (P-09).
     *
     * @throws AppException {@code CANNOT_MODIFY_SELF} for the caller's own account (so the last ADMIN always stays);
     *                      {@code NOT_FOUND} if there is no such user
     */
    @Transactional
    public AdminGetUserResponse disable(UserIdentity admin, long userId) {
        if (admin.id() == userId) throw new AppException(ErrorCode.CANNOT_MODIFY_SELF);
        AdminGetUserResponse user = userStatus.disable(userId);
        sessionRevoker.revokeAllSessions(userId);
        auditLog.record(new AuditEntry(admin, user.storeId(), AuditAction.USER_DISABLED, AuditTargetType.USER, userId,
                Map.of("email", user.email())));
        return user;
    }

    /**
     * @throws AppException {@code NOT_FOUND} if there is no such user
     */
    @Transactional
    public AdminGetUserResponse enable(UserIdentity admin, long userId) {
        AdminGetUserResponse user = userStatus.enable(userId);
        auditLog.record(new AuditEntry(admin, user.storeId(), AuditAction.USER_ENABLED, AuditTargetType.USER, userId,
                Map.of("email", user.email(), "status", user.status())));
        return user;
    }

    /**
     * Logs the user out everywhere ("buộc đăng xuất"): every refresh token revoked.
     *
     * @throws AppException {@code NOT_FOUND} if there is no such user
     */
    @Transactional
    public void revokeSessions(UserIdentity admin, long userId) {
        UserIdentity user = userAccounts.findUserById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        sessionRevoker.revokeAllSessions(userId);
        auditLog.record(new AuditEntry(admin, user.storeId(), AuditAction.USER_SESSIONS_REVOKED, AuditTargetType.USER,
                userId, Map.of("email", user.email())));
    }

    /**
     * Resends the verification code to an account waiting for verification (T57). Not in a transaction: the email is
     * sent first, then the action is recorded.
     *
     * @throws AppException {@code NOT_FOUND}, {@code EMAIL_ALREADY_VERIFIED}, {@code ACCOUNT_DISABLED},
     *                      {@code RATE_LIMITED}, {@code OTP_COOLDOWN}
     */
    public void sendVerificationEmail(UserIdentity admin, long userId) {
        UserIdentity user = userAccounts.findUserById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        accountSupport.sendVerificationEmail(userId);
        auditLog.record(new AuditEntry(admin, user.storeId(), AuditAction.VERIFICATION_EMAIL_SENT, AuditTargetType.USER,
                userId, Map.of("email", user.email())));
    }

    /**
     * Emails a password reset link (T57, P-63); the ADMIN never sees it nor any password.
     *
     * @throws AppException {@code NOT_FOUND}, {@code ACCOUNT_DISABLED}, {@code EMAIL_NOT_VERIFIED},
     *                      {@code RATE_LIMITED}
     */
    public void sendPasswordResetLink(UserIdentity admin, long userId) {
        UserIdentity user = userAccounts.findUserById(userId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        accountSupport.sendPasswordResetLink(userId);
        auditLog.record(new AuditEntry(admin, user.storeId(), AuditAction.PASSWORD_RESET_LINK_SENT, AuditTargetType.USER,
                userId, Map.of("email", user.email())));
    }
}
