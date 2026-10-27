package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;

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
     * @throws AppException {@code USER_NOT_FOUND} if the user is not in the caller's store;
     *                      {@code CANNOT_MODIFY_OWNER} for a role or status change of the OWNER
     */
    @Transactional
    public MemberResponse updateMember(UserIdentity actor, long userId, UpdateMemberRequest request) {
        if (request == null) throw new NullPointerException("Update member request could not be null");
        long storeId = StoreService.storeIdOf(actor);
        StoreUser member = storeUserService.findInStore(storeId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (member.role() == UserRole.OWNER && (request.role() != null || request.status() != null))
            throw new AppException(ErrorCode.CANNOT_MODIFY_OWNER);

        StoreUser updated = storeUserService.updateMember(storeId,
                userId,
                request.role() == null ? null : request.role().toUserRole(),
                request.status() == null ? null : request.status().toUserStatus(),
                request.canDrive());
        if (member.status() != UserStatus.DISABLED && updated.status() == UserStatus.DISABLED) {
            eventPublisher.publishEvent(new MemberDisabledEvent(storeId, userId));
            log.info("Member disabled. storeId={}, userId={}", storeId, userId);
        }
        return storeMapper.toMemberResponse(updated);
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
