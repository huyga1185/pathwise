package uk.huy.pathwise.core.phonenumber;

import org.junit.jupiter.api.Test;

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

    @Test
    void toE164_whenValidNationalNumber_returnE164() {
        assertEquals("+84905123456", phoneNumberProcessor.toE164("0905 123 456"));
    }

    @Test
    void toNational_whenE164_returnNationalFormat() {
        assertEquals("0905 123 456", phoneNumberProcessor.toNational("+84905123456"));
    }

    @Test
    void toE164AndToNational_whenNull_returnNull() {
        assertNull(phoneNumberProcessor.toE164(null));
        assertNull(phoneNumberProcessor.toNational(null));
    }

    @Test
    void toE164AndToNational_whenNotAPhoneNumber_throwIllegalArgumentWithoutTheNumber() {
        IllegalArgumentException e164 = assertThrows(IllegalArgumentException.class, () -> phoneNumberProcessor.toE164("abc"));
        IllegalArgumentException national = assertThrows(IllegalArgumentException.class, () -> phoneNumberProcessor.toNational("abc"));
        assertFalse(e164.getMessage().contains("abc"));
        assertFalse(national.getMessage().contains("abc"));
    }
}
