package uk.huy.pathwise.support;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Creates accounts through the public API, like a real OWNER: register, read the OTP from {@link FakeEmailSender},
 * verify the email.
 */
public final class TestAccounts {
    private static final Pattern OTP_IN_EMAIL = Pattern.compile(">(\\d{6})<");

    private TestAccounts() {}

    /**
     * Registers an OWNER (with a new store) and verifies the email, so the account can log in.
     */
    public static void registerVerifiedOwner(MockMvc mockMvc, FakeEmailSender emailSender, String email, String password)
            throws Exception {
        register(mockMvc, email, password, "Cửa hàng " + email);
        mockMvc.perform(post("/api/v1/auth/email/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"otp\":\"" + lastOtp(emailSender, email) + "\"}"))
                .andExpect(status().isNoContent());
    }

    /**
     * Registers an OWNER and leaves the email unverified.
     */
    public static void register(MockMvc mockMvc, String email, String password, String storeName) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password
                                + "\",\"storeName\":\"" + storeName + "\"}"))
                .andExpect(status().isCreated());
    }

    /**
     * @return the OTP of the last email sent to {@code email}
     */
    public static String lastOtp(FakeEmailSender emailSender, String email) {
        Matcher otp = OTP_IN_EMAIL.matcher(emailSender.sentTo(email).getLast().content());
        if (!otp.find()) throw new AssertionError("No OTP in the last email to " + email);
        return otp.group(1);
    }
}
