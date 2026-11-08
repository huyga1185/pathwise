package uk.huy.pathwise.notification;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.huy.pathwise.notification.service.EmailNotificationService;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailNotificationServiceIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private FakeEmailSender emailSender;

    @ParameterizedTest
    @CsvSource({
            "verify-email, Mã xác thực email - PathWise, xác thực địa chỉ email",
            "reset-password, Mã đặt lại mật khẩu - PathWise, đặt lại mật khẩu",
            "change-password, Mã xác nhận đổi mật khẩu - PathWise, đổi mật khẩu"})
    void send_whenOtpTemplate_renderVietnameseWithOtpAndIdentity(String template, String subject, String purposeText) {
        String to = "mail-" + UUID.randomUUID() + "@pathwise.test";

        emailNotificationService.send(template, to, Map.of("otp", "004217", "validMinutes", 5L));

        FakeEmailSender.SentEmail sent = emailSender.sentTo(to).getFirst();
        assertThat(sent.subject()).isEqualTo(subject);
        assertThat(sent.from()).isEqualTo("no-reply@pathwise.test");
        assertThat(sent.name()).isEqualTo("PathWise Test");
        assertThat(sent.isHtml()).isTrue();
        assertThat(sent.content())
                .contains(">004217<", "<strong>5</strong> phút", purposeText, to, "lang=\"vi\"")
                .doesNotContain("${", "seconds", "login request");
    }

    @Test
    void send_whenUnknownTemplate_throwIllegalArgument() {
        assertThatThrownBy(() -> emailNotificationService.send("no-such-template", "a@pathwise.test", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
