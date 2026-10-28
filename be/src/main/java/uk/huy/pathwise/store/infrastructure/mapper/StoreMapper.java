package uk.huy.pathwise.store.infrastructure.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.store.dto.StoreMember;
import uk.huy.pathwise.store.dto.StoreProfile;
import uk.huy.pathwise.store.dto.response.DepotResponse;
import uk.huy.pathwise.store.dto.response.MemberResponse;
import uk.huy.pathwise.store.dto.response.StoreResponse;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.user.dto.StoreUser;

@Mapper(componentModel = "spring",
        imports = UserIdentity.class,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface StoreMapper {
    default StoreResponse toStoreResponse(Store store) {
        DepotResponse depot = store.getDepotLat() == null || store.getDepotLon() == null ? null
                : new DepotResponse(store.getDepotAddress(), store.getDepotLat(), store.getDepotLon());
        return new StoreResponse(store.getId(), store.getName(), depot, store.getTimezone());
    }

    StoreProfile toStoreProfile(Store store);

    StoreMember toStoreMember(StoreUser user);

    @Mapping(target = "canDrive", expression = "java(UserIdentity.canBeAssignedRoutes(user.role(), user.canDrive()))")
    MemberResponse toMemberResponse(StoreUser user);
}
