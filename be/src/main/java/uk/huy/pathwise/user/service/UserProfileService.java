package uk.huy.pathwise.user.service;

import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.GetUserResponse;

public interface UserProfileService {
    void updateUser(long id, UserModificationRequest request);
    GetUserResponse getUser(long id);
}
