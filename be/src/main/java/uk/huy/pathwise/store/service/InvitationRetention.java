package uk.huy.pathwise.store.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.huy.pathwise.core.housekeeping.RetentionTask;
import uk.huy.pathwise.store.repository.StoreInvitationRepository;

import java.time.Instant;

/**
 * Housekeeping of {@code store_invitations} (T53): expired or revoked invitations, 90 days after (by default).
 * They then leave the invitation list of the store; accepted ones stay.
 */
@Component
@RequiredArgsConstructor
class InvitationRetention implements RetentionTask {
    private final StoreInvitationRepository invitationRepository;

    @Override
    public String name() {
        return "invitations";
    }

    @Override
    @Transactional
    public int purge(Instant cutoff) {
        return invitationRepository.deleteUnusableBefore(cutoff);
    }
}
