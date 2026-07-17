package uk.huy.pathwise.shared.phonenumber;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PhoneNumberServiceTest {
    final PhoneNumberService phoneNumberService = new PhoneNumberService();

    @Test
    void check_rawNull_returnNotAPhoneNumber() {
        assertEquals(PhoneCheck.NOT_A_PHONE_NUMBER, phoneNumberService.check(null));
    }

    @Test
    void check_rawEmpty_returnNotAPhoneNumber() {
        assertEquals(PhoneCheck.NOT_A_PHONE_NUMBER, phoneNumberService.check(""));
    }

    @Test
    void check_rawTooLong_returnTooLong() {
        assertEquals(PhoneCheck.TOO_LONG, phoneNumberService.check("0842594991232323232"));
    }

    @Test
    void check_rawTooShort_returnTooShort() {
        assertEquals(PhoneCheck.TOO_SHORT, phoneNumberService.check("+849"));
    }

    @Test
    void check_rawWrongRegion_returnWrongRegion() {
        assertEquals(PhoneCheck.WRONG_REGION, phoneNumberService.check("+1842366252"));
    }

    @Test
    void check_rawWrongType_returnWrongPhoneNumberType() {
        assertEquals(PhoneCheck.WRONG_PHONE_NUMBER_TYPE, phoneNumberService.check("19008080"));
    }

    @Test
    void check_rawValidNumber_returnValid() {
        assertEquals(PhoneCheck.VALID, phoneNumberService.check("+84942594999"));
    }
}
