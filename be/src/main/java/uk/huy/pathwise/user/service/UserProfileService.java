package uk.huy.pathwise.user.service;

import org.springframework.data.domain.Pageable;
import uk.huy.pathwise.core.pagination.PageResponse;
import uk.huy.pathwise.user.dto.request.UpdateProfileRequest;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.shared.identity.UserIdentity;

public interface UserProfileService {
    /**
     * @return the profile after the change
     */
    GetUserResponse updateUser(UserIdentity identity, UpdateProfileRequest request);
    GetUserResponse getUser(UserIdentity identity);
    /**
     * Every user (ADMIN), each filter applied only when not null (T55).
     *
     * @throws uk.huy.pathwise.core.exception.AppException {@code INVALID_SORT_FIELD} outside id, email, createdAt,
     *                                                     updatedAt
     */
    PageResponse<AdminGetUserResponse> getListUser(Long storeId, UserRole role, UserStatus status, Pageable pageable);
}
