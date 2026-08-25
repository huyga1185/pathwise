package uk.huy.pathwise.shared.phonenumber;

import org.junit.jupiter.api.Test;
import uk.huy.pathwise.user.infrastructure.phonenumber.PhoneCheck;
import uk.huy.pathwise.user.infrastructure.phonenumber.PhoneNumberProcessor;

import static org.junit.jupiter.api.Assertions.*;

public class PhoneNumberProcessorTest {
    final PhoneNumberProcessor phoneNumberProcessor = new PhoneNumberProcessor();

    @Test
    void check_rawNull_returnNotAPhoneNumber() {
        assertEquals(PhoneCheck.NOT_A_PHONE_NUMBER, phoneNumberProcessor.check(null));
    }

    @Test
    void check_rawEmpty_returnNotAPhoneNumber() {
        assertEquals(PhoneCheck.NOT_A_PHONE_NUMBER, phoneNumberProcessor.check(""));
    }

    @Test
    void check_rawTooLong_returnTooLong() {
        assertEquals(PhoneCheck.TOO_LONG, phoneNumberProcessor.check("0842594991232323232"));
    }

    @Test
    void check_rawTooShort_returnTooShort() {
        assertEquals(PhoneCheck.TOO_SHORT, phoneNumberProcessor.check("+849"));
    }

    @Test
    void check_rawWrongRegion_returnWrongRegion() {
        assertEquals(PhoneCheck.WRONG_REGION, phoneNumberProcessor.check("+1842366252"));
    }

    @Test
    void check_rawWrongType_returnWrongPhoneNumberType() {
        assertEquals(PhoneCheck.WRONG_PHONE_NUMBER_TYPE, phoneNumberProcessor.check("19008080"));
    }

    @Test
    void check_rawValidNumber_returnValid() {
        assertEquals(PhoneCheck.VALID, phoneNumberProcessor.check("+84942594999"));
    }
}
