package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.audit.dto.AuditEntry;
import uk.huy.pathwise.audit.model.AuditAction;
import uk.huy.pathwise.audit.model.AuditTargetType;
import uk.huy.pathwise.audit.service.AuditLog;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.store.dto.request.UpdateMemberRequest;
import uk.huy.pathwise.store.dto.response.MemberResponse;
import uk.huy.pathwise.store.event.MemberDisabledEvent;
import uk.huy.pathwise.store.infrastructure.mapper.StoreMapper;
import uk.huy.pathwise.user.dto.StoreUser;
import uk.huy.pathwise.user.service.StoreUserService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Members of the caller's store (FR-S04, FR-S05, FR-S06). The store always comes from the access token; a user of
 * another store is reported as not found (BR-01).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MemberService {
    private final StoreUserService storeUserService;
    private final StoreMapper storeMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final AuditLog auditLog;

    /**
     * @param role       only members with this role, if set
     * @param status     only members with this status, if set
     * @param assignable if true, only ACTIVE members who can be assigned routes (DRIVER or {@code canDrive}, D-21)
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> listMembers(UserIdentity actor, UserRole role, UserStatus status, boolean assignable) {
        return storeUserService.listInStore(StoreService.storeIdOf(actor)).stream()
                .filter(member -> role == null || member.role() == role)
                .filter(member -> status == null || member.status() == status)
                .filter(member -> !assignable || (member.status() == UserStatus.ACTIVE
                        && UserIdentity.canBeAssignedRoutes(member.role(), member.canDrive())))
                .map(storeMapper::toMemberResponse)
                .toList();
    }

    /**
     * Changes status, role or {@code canDrive} of a member. Disabling revokes every session of the member in the
     * same transaction (P-09, P-27). A role change reaches the member's access token within 14 minutes (P-09).
     *
     * @throws AppException {@code NOT_FOUND} if the user is not in the caller's store;
     *                      {@code CANNOT_MODIFY_OWNER} for a role or status change of the OWNER
     */
    @Transactional
    public MemberResponse updateMember(UserIdentity actor, long userId, UpdateMemberRequest request) {
        if (request == null) throw new NullPointerException("Update member request could not be null");
        long storeId = StoreService.storeIdOf(actor);
        StoreUser member = storeUserService.findInStore(storeId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (member.role() == UserRole.OWNER && (request.getRole() != null || request.getStatus() != null))
            throw new AppException(ErrorCode.CANNOT_MODIFY_OWNER);

        StoreUser updated = storeUserService.updateMember(storeId,
                userId,
                request.getRole() == null ? null : request.getRole().toUserRole(),
                request.getStatus() == null ? null : request.getStatus().toUserStatus(),
                request.getCanDrive());
        if (member.status() != UserStatus.DISABLED && updated.status() == UserStatus.DISABLED) {
            eventPublisher.publishEvent(new MemberDisabledEvent(storeId, userId));
            log.info("Member disabled. storeId={}, userId={}", storeId, userId);
        }
        Map<String, Object> changes = changes(member, updated);
        if (!changes.isEmpty()) {
            auditLog.record(new AuditEntry(actor, storeId, AuditAction.MEMBER_UPDATED, AuditTargetType.USER, userId,
                    Map.of("email", updated.email(), "changes", changes)));
        }
        return storeMapper.toMemberResponse(updated);
    }

    /**
     * @return {@code field → {from, to}} for each of role, status and canDrive that changed (T58)
     */
    private static Map<String, Object> changes(StoreUser before, StoreUser after) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (before.role() != after.role()) changes.put("role", Map.of("from", before.role(), "to", after.role()));
        if (before.status() != after.status()) changes.put("status", Map.of("from", before.status(), "to", after.status()));
        if (before.canDrive() != after.canDrive())
            changes.put("canDrive", Map.of("from", before.canDrive(), "to", after.canDrive()));
        return changes;
    }

    /**
     * The OWNER turns driving on or off for themself (FR-S06).
     */
    @Transactional
    public MemberResponse updateOwnCanDrive(UserIdentity actor, boolean canDrive) {
        return storeMapper.toMemberResponse(
                storeUserService.updateMember(StoreService.storeIdOf(actor), actor.id(), null, null, canDrive));
    }
}
