package uk.huy.pathwise.user.service;

import org.springframework.data.domain.Pageable;
import uk.huy.pathwise.shared.pagination.PageResponse;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.dto.response.GetUserResponse;

public interface UserProfileService {
    void updateUser(long id, UserModificationRequest request);
    GetUserResponse getUser(long id);
    PageResponse<AdminGetUserResponse> getListUser(Pageable pageable);
}
