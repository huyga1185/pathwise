package uk.huy.pathwise.core.phonenumber;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * Vietnamese phone numbers (P-29). Callers validate with {@link #check(String)} (or {@link ValidPhoneNumber}) before
 * converting. The conversions never expose libphonenumber's checked {@link NumberParseException}: a number that cannot
 * be parsed at that point is a bug or corrupt stored data, reported as {@link IllegalArgumentException} (500, P-24).
 */
@Component
public class PhoneNumberProcessor {
    private final PhoneNumberUtil phoneNumberUtil;
    private static final String REGION = "VN";
    private static final Set<PhoneNumberUtil.PhoneNumberType> ALLOWED = EnumSet.of(
            PhoneNumberUtil.PhoneNumberType.MOBILE,
            PhoneNumberUtil.PhoneNumberType.FIXED_LINE
    );

    public PhoneNumberProcessor() {
        phoneNumberUtil = PhoneNumberUtil.getInstance();
    }

    private Phonenumber.PhoneNumber parseToPhoneNumber(String raw) throws NumberParseException {
        return phoneNumberUtil.parse(raw, REGION);
    }

    /**
     * @param raw a number accepted by {@link #check(String)}, or null
     * @throws IllegalArgumentException if {@code raw} is not a phone number
     */
    @Named("toE164")
    public String toE164(String raw) {
        if (raw == null) return null;
        return format(raw, PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    /**
     * @param e164 a stored E.164 number, or null
     * @throws IllegalArgumentException if {@code e164} is not a phone number
     */
    @Named("toNational")
    public String toNational(String e164) {
        if (e164 == null) return null;
        return format(e164, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    }

    private String format(String number, PhoneNumberUtil.PhoneNumberFormat format) {
        try {
            return phoneNumberUtil.format(parseToPhoneNumber(number), format);
        } catch (NumberParseException e) {
            // No number in the message: it is personal data.
            throw new IllegalArgumentException("Phone number could not be parsed: " + e.getErrorType(), e);
        }
    }

    public PhoneCheck check(String raw) {
        try {
            Phonenumber.PhoneNumber parsedPhoneNumber = parseToPhoneNumber(raw);
            if (!phoneNumberUtil.isValidNumberForRegion(parsedPhoneNumber, REGION)) return PhoneCheck.WRONG_REGION;
            if (!ALLOWED.contains(phoneNumberUtil.getNumberType(parsedPhoneNumber))) return PhoneCheck.WRONG_PHONE_NUMBER_TYPE;
            return PhoneCheck.VALID;
        } catch (NumberParseException e) {
            return switch (e.getErrorType()) {
                case INVALID_COUNTRY_CODE -> PhoneCheck.WRONG_REGION;
                case TOO_LONG -> PhoneCheck.TOO_LONG;
                case TOO_SHORT_NSN, TOO_SHORT_AFTER_IDD -> PhoneCheck.TOO_SHORT;
                default -> PhoneCheck.NOT_A_PHONE_NUMBER;
            };
        }
    }
}
