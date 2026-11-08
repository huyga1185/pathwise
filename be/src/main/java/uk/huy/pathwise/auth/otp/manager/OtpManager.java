package uk.huy.pathwise.auth.otp.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.infrastructure.config.OtpProperties;
import uk.huy.pathwise.auth.otp.infrastructure.generator.OtpGenerator;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.auth.otp.store.OtpStore;
import uk.huy.pathwise.notification.service.EmailDeliveryException;
import uk.huy.pathwise.notification.service.EmailNotificationService;

import java.util.Map;

/**
 * Issues and verifies OTPs (ARCHITECTURE.md §6.1): generate, store the hash, email the code with the template of
 * the purpose, and verify. Keys use the user id, not the email (which can change).
 */
@Component
@Slf4j
@EnableConfigurationProperties(OtpProperties.class)
public class OtpManager {
    private static final Map<OtpPurpose, String> TEMPLATES = Map.of(
            OtpPurpose.VERIFY_EMAIL, EmailNotificationService.VERIFY_EMAIL,
            OtpPurpose.RESET_PASSWORD, EmailNotificationService.RESET_PASSWORD,
            OtpPurpose.CHANGE_PASSWORD, EmailNotificationService.CHANGE_PASSWORD);

    private final OtpStore otpStore;
    private final OtpGenerator otpGenerator;
    private final Hasher hasher;
    private final EmailNotificationService emailNotificationService;
    private final OtpProperties properties;

    public OtpManager(OtpStore otpStore,
                      OtpGenerator otpGenerator,
                      Hasher hasher,
                      EmailNotificationService emailNotificationService,
                      OtpProperties properties) {
        this.otpStore = otpStore;
        this.otpGenerator = otpGenerator;
        this.hasher = hasher;
        this.emailNotificationService = emailNotificationService;
        this.properties = properties;
    }

    /**
     * Generates an OTP for {@code userId} and emails it to {@code email}.
     *
     * @return {@link OtpSendResult#COOLDOWN} if one was sent less than {@code otp.cooldown} ago (nothing sent)
     * @throws EmailDeliveryException if the email could not be sent; the OTP and the cooldown are then removed, so
     *                                the user can ask again right away (F-10)
     */
    public OtpSendResult sendOtp(OtpPurpose purpose, long userId, String email) {
        if (!otpStore.tryStartCooldown(purpose, userId, properties.cooldown())) return OtpSendResult.COOLDOWN;

        String otp = otpGenerator.generateOtp(properties.length());
        otpStore.save(purpose, userId, hasher.hashString(otp), properties.ttl());
        try {
            emailNotificationService.send(TEMPLATES.get(purpose), email,
                    Map.of("otp", otp, "validMinutes", Math.max(1, properties.ttl().toMinutes())));
        } catch (EmailDeliveryException e) {
            otpStore.discard(purpose, userId);
            log.warn("OTP email not sent, OTP and cooldown released. purpose={}, userId={}", purpose, userId);
            throw e;
        }
        return OtpSendResult.SENT;
    }

    /**
     * @return true only if {@code rawOtp} is the current OTP of this purpose and user; it is then consumed
     */
    public boolean verifyOtp(OtpPurpose purpose, long userId, String rawOtp) {
        if (rawOtp == null || rawOtp.isBlank()) return false;
        return otpStore.verify(purpose, userId, hasher.hashString(rawOtp), properties.maxAttempts());
    }
}
