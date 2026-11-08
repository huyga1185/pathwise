package uk.huy.pathwise.user.infrastructure.mapper;

import org.mapstruct.*;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.user.model.User;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.user.dto.StoreUser;

@Mapper(componentModel = "spring",
        imports = UserIdentity.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = "canDrive", expression = "java(UserIdentity.canBeAssignedRoutes(user.getRole(), user.isCanDrive()))")
    GetUserResponse toGetUserResponse(User user);

    AdminGetUserResponse toAdminGetUserResponse(User user);

    UserIdentity toUserIdentity(User user);

    StoreUser toStoreUser(User user);
}
