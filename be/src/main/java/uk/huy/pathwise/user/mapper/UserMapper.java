package uk.huy.pathwise.user.mapper;

import org.mapstruct.*;
import uk.huy.pathwise.shared.phonenumber.PhoneNumberService;
import uk.huy.pathwise.user.dto.request.UserModificationRequest;
import uk.huy.pathwise.user.dto.response.AdminGetUserResponse;
import uk.huy.pathwise.user.dto.response.GetUserResponse;
import uk.huy.pathwise.user.model.User;
import uk.huy.pathwise.shared.identity.UserIdentity;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        uses = PhoneNumberService.class)
public interface UserMapper {
    @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "toE164")
    void update(@MappingTarget User user, UserModificationRequest request);

    @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "toNational")
    GetUserResponse toGetUserResponse(User user);

    @Mapping(target = "phoneNumber", source = "phoneNumber", qualifiedByName = "toNational")
    AdminGetUserResponse toAdminGetUserResponse(User user);

    UserIdentity toUserIdentity(User user);
}
