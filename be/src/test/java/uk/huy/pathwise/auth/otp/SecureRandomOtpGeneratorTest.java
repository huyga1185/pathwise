package uk.huy.pathwise.auth.otp;

import org.junit.jupiter.api.Test;
import uk.huy.pathwise.auth.otp.infrastructure.generator.SecureRandomOtpGenerator;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureRandomOtpGeneratorTest {
    private final SecureRandomOtpGenerator generator = new SecureRandomOtpGenerator();

    @Test
    void generateOtp_whenSizeSix_returnSixDigitsIncludingLeadingZeros() {
        Set<Character> firstDigits = new HashSet<>();
        for (int i = 0; i < 2000; i++) {
            String otp = generator.generateOtp(6);
            assertThat(otp).hasSize(6).containsOnlyDigits();
            firstDigits.add(otp.charAt(0));
        }
        assertThat(firstDigits).contains('0');
    }

    @Test
    void generateOtp_whenSizeOutOfRange_throwIllegalArgument() {
        assertThatThrownBy(() -> generator.generateOtp(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> generator.generateOtp(10)).isInstanceOf(IllegalArgumentException.class);
    }
}
