package uk.huy.pathwise.shared.phonenumber;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.mapstruct.Named;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class PhoneNumberService {
    private final PhoneNumberUtil phoneNumberUtil;
    private static final String REGION = "VN";
    private static final Set<PhoneNumberUtil.PhoneNumberType> ALLOWED = EnumSet.of(
            PhoneNumberUtil.PhoneNumberType.MOBILE,
            PhoneNumberUtil.PhoneNumberType.FIXED_LINE
    );

    public PhoneNumberService() {
        phoneNumberUtil = PhoneNumberUtil.getInstance();
    }

    private Phonenumber.PhoneNumber parseToPhoneNumber(String raw) throws NumberParseException {
        return phoneNumberUtil.parse(raw, REGION);
    }

    @Named("toE164")
    public String toE164(String raw) throws NumberParseException {
        return phoneNumberUtil.format(
                parseToPhoneNumber(raw),
                PhoneNumberUtil.PhoneNumberFormat.E164);
    }

    public String toNational(String e164) throws NumberParseException {
        return phoneNumberUtil.format(
                parseToPhoneNumber(e164),
                PhoneNumberUtil.PhoneNumberFormat.NATIONAL);
    }

    public boolean isValid(String raw) {
        try {
            Phonenumber.PhoneNumber parsedPhoneNumber = parseToPhoneNumber(raw);
            return phoneNumberUtil.isValidNumberForRegion(parsedPhoneNumber, REGION)
                    && ALLOWED.contains(phoneNumberUtil.getNumberType(parsedPhoneNumber));
        } catch (NumberParseException ignored) {
            return false;
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
