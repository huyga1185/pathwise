package uk.huy.pathwise.store.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.core.exception.AppException;
import uk.huy.pathwise.core.exception.ErrorCode;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.core.security.OpaqueTokenGenerator;
import uk.huy.pathwise.notification.service.EmailDeliveryException;
import uk.huy.pathwise.notification.service.EmailNotificationService;
import uk.huy.pathwise.shared.identity.UserIdentity;
import uk.huy.pathwise.shared.identity.UserRole;
import uk.huy.pathwise.store.dto.request.AcceptInvitationRequest;
import uk.huy.pathwise.store.dto.request.CreateInvitationRequest;
import uk.huy.pathwise.store.dto.response.InvitationPreviewResponse;
import uk.huy.pathwise.store.dto.response.InvitationResponse;
import uk.huy.pathwise.store.model.InvitationStatus;
import uk.huy.pathwise.store.model.Store;
import uk.huy.pathwise.store.model.StoreInvitation;
import uk.huy.pathwise.store.repository.StoreInvitationRepository;
import uk.huy.pathwise.store.repository.StoreRepository;
import uk.huy.pathwise.user.dto.StoreUser;
import uk.huy.pathwise.user.service.StoreUserService;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Invitations of DISPATCHERs and DRIVERs (D-16, FR-S02, FR-S03). The code is a random opaque token sent by email;
 * only its SHA-256 is stored. An invitation is valid for 7 days (D-27) and used once.
 * <p>
 * The invitation is saved in a transaction and the email sent after the commit (ARCHITECTURE.md §7). If the email
 * fails the invitation stays and the OWNER invites again, which revokes the previous one.
 */
@Service
@Slf4j
public class InvitationService {
    static final Duration INVITATION_TTL = Duration.ofDays(7);
    private static final DateTimeFormatter EMAIL_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final StoreInvitationRepository invitationRepository;
    private final StoreRepository storeRepository;
    private final StoreUserService storeUserService;
    private final EmailNotificationService emailNotificationService;
    private final OpaqueTokenGenerator codeGenerator;
    private final Hasher hasher;
    private final TransactionTemplate transactionTemplate;

    public InvitationService(StoreInvitationRepository invitationRepository,
                             StoreRepository storeRepository,
                             StoreUserService storeUserService,
                             EmailNotificationService emailNotificationService,
                             OpaqueTokenGenerator codeGenerator,
                             Hasher hasher,
                             PlatformTransactionManager transactionManager) {
        this.invitationRepository = invitationRepository;
        this.storeRepository = storeRepository;
        this.storeUserService = storeUserService;
        this.emailNotificationService = emailNotificationService;
        this.codeGenerator = codeGenerator;
        this.hasher = hasher;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    private record Created(StoreInvitation invitation, Store store, String code) {}

    /**
     * Invites {@code email}; a pending invitation of the same email in this store is revoked first.
     *
     * @throws AppException {@code EMAIL_EXISTED} if the email already belongs to a user, of any store (D-02)
     */
    public InvitationResponse createInvitation(UserIdentity actor, CreateInvitationRequest request) {
        if (request == null) throw new NullPointerException("Create invitation request could not be null");
        long storeId = StoreService.storeIdOf(actor);
        String email = normalizeEmail(request.email());
        Created created = transactionTemplate.execute(status -> {
            if (storeUserService.emailExists(email)) throw new AppException(ErrorCode.EMAIL_EXISTED);
            Store store = storeRepository.findById(storeId).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
            invitationRepository.findAllByStoreIdAndEmailAndStatus(storeId, email, InvitationStatus.PENDING)
                    .forEach(previous -> previous.setStatus(InvitationStatus.REVOKED));
            String code = codeGenerator.generateOpaqueToken();
            StoreInvitation invitation = invitationRepository.save(StoreInvitation.builder()
                    .storeId(storeId)
                    .email(email)
                    .role(request.role().toUserRole())
                    .codeHash(hasher.hashString(code))
                    .status(InvitationStatus.PENDING)
                    .invitedBy(actor.id())
                    .expiresAt(Instant.now().plus(INVITATION_TTL))
                    .build());
            return new Created(invitation, store, code);
        });
        sendInvitationEmail(created);
        return toResponse(created.invitation(), Instant.now());
    }

    /**
     * @param status only invitations in this status (as reported, so {@code EXPIRED} includes expired pending ones)
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> listInvitations(UserIdentity actor, InvitationStatus status) {
        Instant now = Instant.now();
        return invitationRepository.findAllByStoreIdOrderByIdDesc(StoreService.storeIdOf(actor)).stream()
                .filter(invitation -> status == null || invitation.effectiveStatus(now) == status)
                .map(invitation -> toResponse(invitation, now))
                .toList();
    }

    /**
     * @throws AppException {@code NOT_FOUND} if the invitation is not in the caller's store;
     *                      {@code INVITATION_NOT_PENDING} if it was already accepted, revoked or has expired
     */
    @Transactional
    public void revokeInvitation(UserIdentity actor, long invitationId) {
        StoreInvitation invitation = invitationRepository.findByIdAndStoreId(invitationId, StoreService.storeIdOf(actor))
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        if (invitation.effectiveStatus(Instant.now()) != InvitationStatus.PENDING)
            throw new AppException(ErrorCode.INVITATION_NOT_PENDING);
        invitation.setStatus(InvitationStatus.REVOKED);
    }

    /**
     * What the invitee sees before accepting ({@code GET /invitations/{code}}, Should).
     *
     * @throws AppException {@code INVITATION_INVALID} if the code is unknown, used, revoked or expired
     */
    @Transactional(readOnly = true)
    public InvitationPreviewResponse previewInvitation(String code) {
        StoreInvitation invitation = findUsable(code);
        Store store = storeRepository.findById(invitation.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.INVITATION_INVALID));
        return new InvitationPreviewResponse(store.getName(), invitation.getEmail(), invitation.getRole(), invitation.getExpiresAt());
    }

    /**
     * Creates the member account (ACTIVE, email verified because the code came by email) and consumes the code, in
     * one transaction (FR-S03). The member then logs in normally.
     *
     * @throws AppException {@code INVITATION_INVALID} if the code is unknown, used, revoked or expired;
     *                      {@code EMAIL_EXISTED} if the email got an account since the invitation was sent
     */
    @Transactional
    public void acceptInvitation(AcceptInvitationRequest request) {
        if (request == null) throw new NullPointerException("Accept invitation request could not be null");
        StoreInvitation invitation = findUsable(request.code());
        StoreUser member = storeUserService.createMember(invitation.getStoreId(),
                invitation.getEmail(),
                request.password(),
                request.fullName().trim(),
                request.phoneNumber(),
                invitation.getRole());
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedUserId(member.id());
        invitation.setAcceptedAt(Instant.now());
        log.info("Invitation accepted. storeId={}, invitationId={}, userId={}", invitation.getStoreId(), invitation.getId(), member.id());
    }

    private StoreInvitation findUsable(String code) {
        if (code == null || code.isBlank()) throw new AppException(ErrorCode.INVITATION_INVALID);
        return invitationRepository.findByCodeHash(hasher.hashString(code.trim()))
                .filter(invitation -> invitation.effectiveStatus(Instant.now()) == InvitationStatus.PENDING)
                .orElseThrow(() -> new AppException(ErrorCode.INVITATION_INVALID));
    }

    private void sendInvitationEmail(Created created) {
        StoreInvitation invitation = created.invitation();
        ZoneId zone = ZoneId.of(created.store().getTimezone());
        try {
            emailNotificationService.send(EmailNotificationService.INVITATION, invitation.getEmail(), Map.of(
                    "storeName", created.store().getName(),
                    "roleName", invitation.getRole() == UserRole.DRIVER ? "tài xế" : "điều phối viên",
                    "code", created.code(),
                    "expiresAt", EMAIL_DATE.format(invitation.getExpiresAt().atZone(zone)),
                    "validDays", INVITATION_TTL.toDays()));
        } catch (EmailDeliveryException e) {
            log.warn("Invitation email not sent; the OWNER can invite again. storeId={}, invitationId={}",
                    invitation.getStoreId(), invitation.getId());
        }
    }

    private static InvitationResponse toResponse(StoreInvitation invitation, Instant now) {
        return new InvitationResponse(invitation.getId(),
                invitation.getEmail(),
                invitation.getRole(),
                invitation.effectiveStatus(now),
                invitation.getExpiresAt());
    }

    /**
     * Same rule as for user accounts (P-19), so the invitation and the account it creates have the same email.
     */
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
