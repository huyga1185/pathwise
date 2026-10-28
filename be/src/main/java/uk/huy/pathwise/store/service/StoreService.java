package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.store.dto.StoreMember;
import uk.huy.pathwise.store.dto.StoreProfile;
import uk.huy.pathwise.store.dto.request.UpdateStoreRequest;
import uk.huy.pathwise.store.dto.response.StoreResponse;
import uk.huy.pathwise.store.infrastructure.mapper.StoreMapper;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.store.repository.StoreRepository;
import uk.huy.pathwise.user.service.StoreUserService;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * The store of the caller. The store id always comes from the access token ({@code actor.storeId()}, BR-01); an
 * actor without a store (ADMIN) gets 404 like any store it cannot see.
 */
@Service
@RequiredArgsConstructor
public class StoreService implements StoreProvisioningService, StoreMemberQuery, StoreQuery {
    static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;
    private final StoreUserService storeUserService;

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
        return storeUserService.findInStore(storeId, userId)
                .filter(user -> user.status() == UserStatus.ACTIVE)
                .map(storeMapper::toStoreMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreMember> findMembers(long storeId, Collection<Long> userIds) {
        return storeUserService.findAllInStore(storeId, userIds).stream().map(storeMapper::toStoreMember).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoreProfile> findStore(long storeId) {
        return storeRepository.findById(storeId).map(storeMapper::toStoreProfile);
    }

    private Store getStoreOf(UserIdentity actor) {
        return storeRepository.findById(storeIdOf(actor))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    /**
     * @return the caller's store id from the access token
     * @throws AppException {@code NOT_FOUND} for an actor without a store (ADMIN)
     */
    static long storeIdOf(UserIdentity actor) {
        if (actor == null) throw new NullPointerException("Actor could not be null");
        if (actor.storeId() == null) throw new AppException(ErrorCode.NOT_FOUND);
        return actor.storeId();
    }

    private static String parseTimezone(String timezone) {
        try {
            return ZoneId.of(timezone.trim()).getId();
        } catch (DateTimeException e) {
            throw new AppException(ErrorCode.INVALID_TIMEZONE);
        }
    }
}
