package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.shared.store.StoreState;
import uk.huy.pathwise.shared.store.StoreStatus;
import uk.huy.pathwise.store.dto.StoreRecord;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.store.repository.StoreRepository;
import uk.huy.pathwise.user.service.StoreStateLookup;

import java.time.Instant;
import java.util.Optional;

/**
 * The READ_ONLY lock of a store (T56, P-59, P-62): set by an ADMIN, checked before every write of its members.
 */
@Service
@RequiredArgsConstructor
public class StoreLockService implements StoreLockCommand, StoreStateLookup {
    private final StoreRepository storeRepository;

    @Override
    @Transactional
    public StoreRecord lock(long storeId, long adminId, String reason) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        store.setStatus(StoreStatus.READ_ONLY);
        store.setLockReason(reason);
        store.setLockedAt(Instant.now());
        store.setLockedBy(adminId);
        return StoreDirectoryService.toRecord(store);
    }

    @Override
    @Transactional
    public StoreRecord unlock(long storeId) {
        Store store = storeRepository.findById(storeId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        store.setStatus(StoreStatus.ACTIVE);
        store.setLockReason(null);
        store.setLockedAt(null);
        store.setLockedBy(null);
        return StoreDirectoryService.toRecord(store);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoreState> findState(long storeId) {
        return storeRepository.findById(storeId).map(store -> new StoreState(store.getStatus(), store.getLockReason()));
    }

    /**
     * @throws AppException {@code STORE_READ_ONLY} if the store is locked
     */
    @Transactional(readOnly = true)
    public void checkWritable(long storeId) {
        if (storeRepository.findStatusById(storeId).orElse(StoreStatus.ACTIVE) == StoreStatus.READ_ONLY)
            throw new AppException(ErrorCode.STORE_READ_ONLY);
    }
}
