package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.DetailedAppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.exception.FieldErrorDetail;
import uk.huy.pathwise.notification.service.EmailDeliveryException;
import uk.huy.pathwise.notification.service.EmailNotificationService;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.shared.identity.UserStatus;
import uk.huy.pathwise.store.dto.OwnerTransfer;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.store.repository.StoreRepository;
import uk.huy.pathwise.user.dto.StoreUser;
import uk.huy.pathwise.user.service.StoreUserService;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreOwnershipService implements StoreOwnershipCommand {
    private final StoreRepository storeRepository;
    private final StoreUserService storeUserService;
    private final EmailNotificationService emailNotificationService;

    @Override
    @Transactional
    public OwnerTransfer transferOwner(long storeId, long newOwnerId) {
        // Locks the store row: two transfers of the same store run one after the other, so there is one OWNER.
        Store store = storeRepository.findByIdForUpdate(storeId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        StoreUser candidate = storeUserService.findInStore(storeId, newOwnerId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (candidate.role() == UserRole.OWNER) throw invalid("ALREADY_OWNER");
        if (candidate.status() != UserStatus.ACTIVE) throw invalid("NEW_OWNER_NOT_ACTIVE");
        StoreUser previous = storeUserService.listInStore(storeId).stream()
                .filter(member -> member.role() == UserRole.OWNER)
                .findFirst()
                .orElse(null);

        if (previous != null) storeUserService.updateMember(storeId, previous.id(), UserRole.DISPATCHER, null, null);
        // A driver who becomes OWNER keeps their routes on the driver app (P-63).
        Boolean canDrive = candidate.role() == UserRole.DRIVER ? Boolean.TRUE : null;
        StoreUser owner = storeUserService.updateMember(storeId, newOwnerId, UserRole.OWNER, null, canDrive);
        log.info("Store OWNER transferred. storeId={}, from={}, to={}", storeId, previous == null ? null : previous.id(), owner.id());

        emailAfterCommit(store.getName(), owner.email(), previous == null ? null : previous.email());
        return new OwnerTransfer(storeId, previous == null ? null : previous.id(), previous == null ? null : previous.email(),
                owner.id(), owner.email(), owner.canDrive());
    }

    private static DetailedAppException invalid(String code) {
        return new DetailedAppException(ErrorCode.VALIDATION_ERROR, List.of(new FieldErrorDetail("userId", code)));
    }

    /**
     * Emails the new OWNER and, to the old address, the previous OWNER (it does not name the new one). After the
     * commit, never inside the transaction (ARCHITECTURE §7).
     */
    private void emailAfterCommit(String storeName, String newOwnerEmail, String previousOwnerEmail) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send(EmailNotificationService.OWNER_GRANTED, newOwnerEmail, storeName);
                if (previousOwnerEmail != null) send(EmailNotificationService.OWNER_TRANSFERRED, previousOwnerEmail, storeName);
            }
        });
    }

    private void send(String template, String to, String storeName) {
        try {
            emailNotificationService.send(template, to, Map.of("storeName", storeName));
        } catch (EmailDeliveryException e) {
            log.warn("Owner transfer email not sent. template={}", template);
        }
    }
}
