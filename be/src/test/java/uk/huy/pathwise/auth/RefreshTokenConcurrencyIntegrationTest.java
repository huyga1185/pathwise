package uk.huy.pathwise.auth;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.huy.pathwise.auth.model.ClientDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RefreshTokenGenerationDetail;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.dto.RotationResult.Rotated;
import uk.huy.pathwise.auth.token.opaque.refreshtoken.manager.RefreshTokenManager;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestAccounts;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestUser;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-44: MariaDB 11.6+ snapshot isolation fails a transaction (error 1020) that read a row without a lock and later
 * locks it after another transaction changed it. Each test holds a refresh rotation open in one transaction, starts a
 * second operation on the same session, waits until that one waits for the row lock, then lets the rotation commit.
 * Before the fix each test failed with error 1020 ("Record has changed since last read").
 */
@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@AutoConfigureMockMvc
class RefreshTokenConcurrencyIntegrationTest extends AbstractIntegrationTest {
    private static final ClientDetail CLIENT = new ClientDetail("UA", "127.0.0.1");

    @Autowired
    private RefreshTokenManager refreshTokenManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeEmailSender emailSender;

    private ExecutorService pool;
    private TestUser user;
    private long userId;

    @BeforeEach
    void setUp() throws Exception {
        pool = Executors.newFixedThreadPool(2);
        user = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender).createAdmin();
        userId = user.id();
    }

    @AfterEach
    void tearDown() {
        pool.shutdownNow();
    }

    @Test
    void rotateToken_whenUsedTokenWithinGraceRacesRotationOfActiveToken_rotateLatestTokenOnce() throws Exception {
        RefreshTokenGenerationDetail login = refreshTokenManager.generateToken(userId, CLIENT);
        Rotated first = (Rotated) refreshTokenManager.rotateToken(login.token(), CLIENT);

        Future<RotationResult> racing = whileRotationHeld(first.token(),
                () -> refreshTokenManager.rotateToken(login.token(), CLIENT));

        assertThat(racing.get(20, TimeUnit.SECONDS)).isInstanceOf(Rotated.class);
        assertThat(countActive(login.sessionId())).isEqualTo(1);
    }

    @Test
    void revokeSession_whenRotationOfSameSessionCommitsMeanwhile_revokeWholeFamily() throws Exception {
        RefreshTokenGenerationDetail login = refreshTokenManager.generateToken(userId, CLIENT);

        Future<Boolean> racing = whileRotationHeld(login.token(),
                () -> refreshTokenManager.revokeSession(userId, login.sessionId()));

        assertThat(racing.get(20, TimeUnit.SECONDS)).isTrue();
        assertThat(countActive(login.sessionId())).isZero();
    }

    @Test
    void changePassword_whenRefreshOfOtherSessionCommitsMeanwhile_return204AndRevokeIt() throws Exception {
        RefreshTokenGenerationDetail other = refreshTokenManager.generateToken(userId, CLIENT);
        mockMvc.perform(post("/api/v1/auth/password/change/otp")
                        .header(HttpHeaders.AUTHORIZATION, user.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + TestUsers.PASSWORD + "\"}"))
                .andExpect(status().isNoContent());
        String otp = TestAccounts.lastOtp(emailSender, user.email());

        Future<Integer> racing = whileRotationHeld(other.token(), () -> {
            try {
                return mockMvc.perform(put("/api/v1/auth/password")
                                .header(HttpHeaders.AUTHORIZATION, user.bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"otp\":\"" + otp + "\",\"newPassword\":\"N3wPassw0rd!\"}"))
                        .andReturn().getResponse().getStatus();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        });

        assertThat(racing.get(20, TimeUnit.SECONDS)).isEqualTo(204);
        assertThat(countActive(other.sessionId())).isZero();
    }

    /**
     * Rotates {@code rawToken} in a transaction kept open until {@code racing} waits for a row lock, then commits it.
     */
    private <T> Future<T> whileRotationHeld(String rawToken, Supplier<T> racing) throws Exception {
        CountDownLatch rotated = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        Future<?> holder = pool.submit(() -> tx.executeWithoutResult(status -> {
            assertThat(refreshTokenManager.rotateToken(rawToken, CLIENT)).isInstanceOf(Rotated.class);
            entityManager.flush(); // all its writes done, only the commit left
            rotated.countDown();
            await(release);
        }));
        assertThat(rotated.await(10, TimeUnit.SECONDS)).isTrue();

        Future<T> result = pool.submit(racing::get);
        awaitLockWait(result);
        release.countDown();
        holder.get(20, TimeUnit.SECONDS);
        return result;
    }

    private void awaitLockWait(Future<?> racing) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline) && !racing.isDone()) {
            // INNODB_TRX needs the PROCESS privilege; our own connections are visible without it. The rotation's
            // connection sits idle while held, so a statement on refresh_tokens still running after 200 ms is blocked.
            Integer waiting = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.PROCESSLIST "
                    + "WHERE ID <> CONNECTION_ID() AND COMMAND = 'Query' AND TIME_MS > 200 "
                    + "AND INFO LIKE '%refresh_tokens%'", Integer.class);
            if (waiting != null && waiting > 0) return;
            Thread.sleep(20);
        }
        throw new AssertionError("The racing operation never waited for the rotation's row lock");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(20, TimeUnit.SECONDS)) throw new AssertionError("Not released");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private int countActive(String familyId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM refresh_tokens WHERE family_id = ? AND state = 'ACTIVE'", Integer.class, familyId);
    }
}
