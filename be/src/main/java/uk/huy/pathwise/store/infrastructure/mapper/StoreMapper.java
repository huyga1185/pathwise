package uk.huy.pathwise.store.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.store.dto.StoreMember;
import uk.huy.pathwise.store.dto.response.MemberResponse;
import uk.huy.pathwise.store.dto.response.StoreResponse;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.user.dto.StoreUser;

@Mapper(componentModel = "spring",
        imports = UserIdentity.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface StoreMapper {
    StoreResponse toStoreResponse(Store store);

    StoreMember toStoreMember(StoreUser user);

    @Mapping(target = "canDrive", expression = "java(UserIdentity.canBeAssignedRoutes(user.role(), user.canDrive()))")
    MemberResponse toMemberResponse(StoreUser user);
}
