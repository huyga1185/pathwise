package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.store.dto.StoreMember;
import uk.huy.pathwise.store.dto.request.UpdateStoreRequest;
import uk.huy.pathwise.store.dto.response.StoreResponse;
import uk.huy.pathwise.store.infrastructure.mapper.StoreMapper;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.store.repository.StoreRepository;
import uk.huy.pathwise.user.service.StoreUserQuery;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Optional;

/**
 * The store of the caller. The store id always comes from the access token ({@code actor.storeId()}, BR-01); an
 * actor without a store (ADMIN) gets 404 like any store it cannot see.
 */
@Service
@RequiredArgsConstructor
public class StoreService implements StoreProvisioningService, StoreMemberQuery {
    static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final StoreUserQuery storeUserQuery;

    @Override
    @Transactional
    public long createStore(String name) {
        Store store = Store.builder()
                .name(name)
                .timezone(DEFAULT_TIMEZONE)
                .build();
        return storeRepository.save(store).getId();
    }

    @Transactional(readOnly = true)
    public StoreResponse getStore(UserIdentity actor) {
        return storeMapper.toStoreResponse(getStoreOf(actor));
    }

    /**
     * @throws AppException {@code INVALID_TIMEZONE} if {@code timezone} is not a zone id known to Java
     */
    @Transactional
    public StoreResponse updateStore(UserIdentity actor, UpdateStoreRequest request) {
        if (request == null) throw new NullPointerException("Update store request could not be null");
        Store store = getStoreOf(actor);
        store.setName(request.name().trim());
        store.setDepotAddress(request.depotAddress());
        store.setDepotLat(request.depotLat());
        store.setDepotLon(request.depotLon());
        store.setTimezone(parseTimezone(request.timezone()));
        return storeMapper.toStoreResponse(storeRepository.save(store));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoreMember> findActiveMember(long storeId, long userId) {
        return storeUserQuery.findInStore(storeId, userId)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .map(storeMapper::toStoreMember);
    }

    private Store getStoreOf(UserIdentity actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        return storeRepository.findById(actor.storeId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    private static String parseTimezone(String timezone) {
        try {
            return ZoneId.of(timezone.trim()).getId();
        } catch (DateTimeException e) {
            throw new AppException(ErrorCode.INVALID_TIMEZONE);
        }
    }
}
