package uk.huy.pathwise.auth.otp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import uk.huy.pathwise.auth.otp.dto.OtpSendResult;
import uk.huy.pathwise.auth.otp.manager.OtpManager;
import uk.huy.pathwise.auth.otp.model.OtpPurpose;
import uk.huy.pathwise.notification.service.EmailDeliveryException;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;

import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T12 acceptance against a real Redis (Testcontainers). The test profile sets {@code otp.ttl=2m}.
 */
class OtpManagerIntegrationTest extends AbstractIntegrationTest {
    private static final Pattern OTP_IN_EMAIL = Pattern.compile(">(\\d{6})<");

    @Autowired
    private OtpManager otpManager;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private FakeEmailSender emailSender;

    @Test
    void verifyOtp_whenCorrect_returnTrueOnceThenOtpIsConsumed() {
        long userId = newUserId();
        String otp = send(OtpPurpose.CHANGE_PASSWORD, userId);

        assertThat(otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, otp)).isTrue();
        assertThat(otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, otp)).isFalse();
    }

    @Test
    void verifyOtp_whenWrongThreeTimes_cancelOtpSoCorrectCodeFails() {
        long userId = newUserId();
        String otp = send(OtpPurpose.CHANGE_PASSWORD, userId);
        String wrong = otp.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < 3; i++) assertThat(otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, wrong)).isFalse();

        assertThat(redis.hasKey(otpKey(OtpPurpose.CHANGE_PASSWORD, userId))).isFalse();
        assertThat(otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, otp)).isFalse();
    }

    @Test
    void verifyOtp_whenWrongTwiceThenCorrect_returnTrue() {
        long userId = newUserId();
        String otp = send(OtpPurpose.CHANGE_PASSWORD, userId);
        String wrong = otp.equals("000000") ? "111111" : "000000";

        otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, wrong);
        otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, wrong);

        assertThat(otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, otp)).isTrue();
    }

    @Test
    void verifyOtp_whenIssuedForAnotherPurpose_returnFalse() {
        long userId = newUserId();
        String otp = send(OtpPurpose.RESET_PASSWORD, userId);

        assertThat(otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId, otp)).isFalse();
        assertThat(otpManager.verifyOtp(OtpPurpose.RESET_PASSWORD, userId, otp)).isTrue();
    }

    @Test
    void verifyOtp_whenIssuedForAnotherUser_returnFalse() {
        long userId = newUserId();
        String otp = send(OtpPurpose.CHANGE_PASSWORD, userId);

        assertThat(otpManager.verifyOtp(OtpPurpose.CHANGE_PASSWORD, userId + 1, otp)).isFalse();
    }

    @Test
    void sendOtp_whenCooldownRunning_returnCooldownAndSendNothing() {
        long userId = newUserId();
        String email = email(userId);
        send(OtpPurpose.CHANGE_PASSWORD, userId);

        assertThat(otpManager.sendOtp(OtpPurpose.CHANGE_PASSWORD, userId, email)).isEqualTo(OtpSendResult.COOLDOWN);
        assertThat(emailSender.sentTo(email)).hasSize(1);
    }

    @Test
    void sendOtp_whenSent_useConfiguredTtlInRedisAndEmail() {
        long userId = newUserId();
        send(OtpPurpose.CHANGE_PASSWORD, userId);

        Long ttl = redis.getExpire(otpKey(OtpPurpose.CHANGE_PASSWORD, userId));
        assertThat(ttl).isBetween(110L, 120L);
        Long cooldown = redis.getExpire("otp:cooldown:CHANGE_PASSWORD:" + userId);
        assertThat(cooldown).isBetween(50L, 60L);
        assertThat(emailSender.sentTo(email(userId)).getFirst().content()).contains("<strong>2</strong> phút");
    }

    @Test
    void sendOtp_whenEmailFails_releaseCooldownAndOtpSoUserCanRetry() {
        long userId = newUserId();
        emailSender.failNextSend();

        assertThatThrownBy(() -> otpManager.sendOtp(OtpPurpose.CHANGE_PASSWORD, userId, email(userId)))
                .isInstanceOf(EmailDeliveryException.class);

        assertThat(redis.hasKey("otp:cooldown:CHANGE_PASSWORD:" + userId)).isFalse();
        assertThat(redis.hasKey(otpKey(OtpPurpose.CHANGE_PASSWORD, userId))).isFalse();
        assertThat(otpManager.sendOtp(OtpPurpose.CHANGE_PASSWORD, userId, email(userId))).isEqualTo(OtpSendResult.SENT);
    }

    private String send(OtpPurpose purpose, long userId) {
        assertThat(otpManager.sendOtp(purpose, userId, email(userId))).isEqualTo(OtpSendResult.SENT);
        String content = emailSender.sentTo(email(userId)).getLast().content();
        Matcher matcher = OTP_IN_EMAIL.matcher(content);
        assertThat(matcher.find()).as("OTP in email").isTrue();
        return matcher.group(1);
    }

    private static long newUserId() {
        return ThreadLocalRandom.current().nextLong(1_000_000, Long.MAX_VALUE / 2);
    }

    private static String email(long userId) {
        return "otp-" + userId + "@pathwise.test";
    }

    private static String otpKey(OtpPurpose purpose, long userId) {
        return "otp:" + purpose.name() + ":" + userId;
    }
}
