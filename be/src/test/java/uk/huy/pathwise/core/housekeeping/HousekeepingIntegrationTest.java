package uk.huy.pathwise.core.housekeeping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import uk.huy.pathwise.support.AbstractIntegrationTest;
import uk.huy.pathwise.support.FakeEmailSender;
import uk.huy.pathwise.support.TestUsers;
import uk.huy.pathwise.support.TestUsers.TestStore;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T53 acceptance: stale rows are deleted after their retention (30 days for refresh tokens and jobs, 90 for
 * invitations), live ones stay, and only one instance runs at a time.
 */
@AutoConfigureMockMvc
class HousekeepingIntegrationTest extends AbstractIntegrationTest {
    private static final Duration DAY = Duration.ofDays(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender emailSender;

    @Autowired
    private HousekeepingService housekeepingService;

    @Autowired
    private HousekeepingLock housekeepingLock;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private TestStore store;
    private Instant now;

    @BeforeEach
    void setUp() throws Exception {
        store = new TestUsers(mockMvc, jdbcTemplate, passwordEncoder, emailSender).createStore();
        now = Instant.now();
        redisTemplate.delete(RedisHousekeepingLock.KEY);
    }

    @Test
    void run_whenRefreshTokensStale_deleteExpiredAndRevokedOnlyAfterThirtyDays() {
        long expiredLongAgo = refreshToken("ACTIVE", now.minus(DAY.multipliedBy(40)), now.minus(DAY.multipliedBy(40)));
        long revokedLongAgo = refreshToken("REVOKED", now.plus(DAY.multipliedBy(5)), now.minus(DAY.multipliedBy(31)));
        long revokedRecently = refreshToken("REVOKED", now.plus(DAY.multipliedBy(5)), now.minus(DAY.multipliedBy(2)));
        long expiredRecently = refreshToken("USED", now.minus(DAY.multipliedBy(10)), now.minus(DAY.multipliedBy(10)));
        long live = refreshToken("ACTIVE", now.plus(DAY.multipliedBy(14)), now);

        Map<String, Integer> deleted = housekeepingService.run();

        assertThat(deleted).containsKeys("refresh-tokens", "jobs", "invitations");
        assertThat(exists("refresh_tokens", expiredLongAgo)).isFalse();
        assertThat(exists("refresh_tokens", revokedLongAgo)).isFalse();
        assertThat(exists("refresh_tokens", revokedRecently)).isTrue();
        assertThat(exists("refresh_tokens", expiredRecently)).isTrue();
        assertThat(exists("refresh_tokens", live)).isTrue();
    }

    @Test
    void run_whenJobsFinishedLongAgo_deleteThemButKeepRunningRecentAndActiveOnes() {
        long succeededOld = job("SUCCEEDED", now.minus(DAY.multipliedBy(31)));
        long failedOld = job("FAILED", now.minus(DAY.multipliedBy(45)));
        long succeededRecent = job("SUCCEEDED", now.minus(DAY.multipliedBy(5)));
        long running = job("RUNNING", null);
        long activeOfPlan = job("SUCCEEDED", now.minus(DAY.multipliedBy(60)));
        long planId = plan(activeOfPlan);

        housekeepingService.run();

        assertThat(exists("background_jobs", succeededOld)).isFalse();
        assertThat(exists("background_jobs", failedOld)).isFalse();
        assertThat(exists("background_jobs", succeededRecent)).isTrue();
        assertThat(exists("background_jobs", running)).isTrue();
        assertThat(exists("background_jobs", activeOfPlan)).as("still the active job of plan %d", planId).isTrue();
    }

    @Test
    void run_whenInvitationsUnusableLongAgo_deleteThemButKeepAcceptedAndRecent() {
        long expiredOld = invitation("PENDING", now.minus(DAY.multipliedBy(100)), now.minus(DAY.multipliedBy(107)));
        long storedExpiredOld = invitation("EXPIRED", now.minus(DAY.multipliedBy(95)), now.minus(DAY.multipliedBy(95)));
        long revokedOld = invitation("REVOKED", now.plus(DAY.multipliedBy(3)), now.minus(DAY.multipliedBy(91)));
        long expiredRecently = invitation("PENDING", now.minus(DAY.multipliedBy(50)), now.minus(DAY.multipliedBy(57)));
        long acceptedOld = invitation("ACCEPTED", now.minus(DAY.multipliedBy(200)), now.minus(DAY.multipliedBy(205)));
        long pending = invitation("PENDING", now.plus(DAY.multipliedBy(7)), now);

        housekeepingService.run();

        assertThat(exists("store_invitations", expiredOld)).isFalse();
        assertThat(exists("store_invitations", storedExpiredOld)).isFalse();
        assertThat(exists("store_invitations", revokedOld)).isFalse();
        assertThat(exists("store_invitations", expiredRecently)).isTrue();
        assertThat(exists("store_invitations", acceptedOld)).isTrue();
        assertThat(exists("store_invitations", pending)).isTrue();
    }

    @Test
    void run_whenAnotherInstanceHoldsTheLock_deleteNothing() {
        long stale = refreshToken("ACTIVE", now.minus(DAY.multipliedBy(40)), now.minus(DAY.multipliedBy(40)));
        String other = housekeepingLock.tryAcquire(Duration.ofMinutes(5));
        assertThat(other).isNotNull();
        assertThat(redisTemplate.getExpire(RedisHousekeepingLock.KEY, TimeUnit.SECONDS)).isBetween(1L, 300L);

        assertThat(housekeepingService.run()).isEmpty();
        assertThat(exists("refresh_tokens", stale)).isTrue();

        housekeepingLock.release(other);
        housekeepingService.run();
        assertThat(exists("refresh_tokens", stale)).isFalse();
        assertThat(redisTemplate.hasKey(RedisHousekeepingLock.KEY)).as("released after the run").isFalse();
    }

    @Test
    void release_whenLockTakenByAnotherRun_leaveIt() {
        String first = housekeepingLock.tryAcquire(Duration.ofMinutes(5));
        redisTemplate.delete(RedisHousekeepingLock.KEY);
        String second = housekeepingLock.tryAcquire(Duration.ofMinutes(5));

        housekeepingLock.release(first);

        assertThat(second).isNotNull();
        assertThat(redisTemplate.hasKey(RedisHousekeepingLock.KEY)).isTrue();
        housekeepingLock.release(second);
    }

    private long refreshToken(String state, Instant expiresAt, Instant updatedAt) {
        return insert("INSERT INTO refresh_tokens (token, user_id, family_id, state, expires_at, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", ""),
                store.owner().id(), UUID.randomUUID().toString(), state, ts(expiresAt), ts(updatedAt), ts(updatedAt));
    }

    private long job(String status, Instant finishedAt) {
        return insert("INSERT INTO background_jobs (store_id, type, target_id, status, attempts, created_by, finished_at, created_at, updated_at) "
                        + "VALUES (?, 'OPTIMIZE_PLAN', 0, ?, 1, ?, ?, ?, ?)",
                store.id(), status, store.owner().id(), finishedAt == null ? null : ts(finishedAt),
                ts(now.minus(DAY.multipliedBy(90))), ts(now.minus(DAY.multipliedBy(90))));
    }

    /**
     * A plan of the store with {@code activeJobId} as its active job.
     */
    private long plan(long activeJobId) {
        return insert("INSERT INTO route_plans (store_id, name, plan_date, status, processing_status, active_job_id, "
                        + "depot_lat, depot_lon, timezone, version, created_by, created_at, updated_at) "
                        + "VALUES (?, 'Cũ', '2026-01-01', 'DRAFT', 'OPTIMIZING', ?, 16.05, 108.2, 'Asia/Ho_Chi_Minh', 0, ?, ?, ?)",
                store.id(), activeJobId, store.owner().id(), ts(now), ts(now));
    }

    private long invitation(String status, Instant expiresAt, Instant updatedAt) {
        return insert("INSERT INTO store_invitations (store_id, email, role, code_hash, status, invited_by, expires_at, created_at, updated_at) "
                        + "VALUES (?, ?, 'DRIVER', ?, ?, ?, ?, ?, ?)",
                store.id(), UUID.randomUUID() + "@pathwise.test",
                (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "").substring(0, 64),
                status, store.owner().id(), ts(expiresAt), ts(updatedAt), ts(updatedAt));
    }

    private long insert(String sql, Object... args) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            return statement;
        }, keys);
        return keys.getKey().longValue();
    }

    private boolean exists(String table, long id) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id) == 1;
    }

    private static Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }
}
