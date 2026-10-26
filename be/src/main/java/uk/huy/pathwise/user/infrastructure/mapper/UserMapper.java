package uk.huy.pathwise.user.infrastructure.mapper;

import org.mapstruct.*;
import uk.huy.pathwise.user.infrastructure.phonenumber.PhoneNumberProcessor;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.user.model.User;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.user.dto.StoreUser;

@Mapper(componentModel = "spring",
        imports = UserRole.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        uses = PhoneNumberProcessor.class)
public interface UserMapper {
    @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "toE164")
    void update(@MappingTarget User user, UserModificationRequest request);

    @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "toNational")
    @Mapping(target = "canDrive", expression = "java(user.getRole() == UserRole.DRIVER || user.isCanDrive())")
    GetUserResponse toGetUserResponse(User user);

    @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "toNational")
    AdminGetUserResponse toAdminGetUserResponse(User user);

    UserIdentity toUserIdentity(User user);

    StoreUser toStoreUser(User user);
}
