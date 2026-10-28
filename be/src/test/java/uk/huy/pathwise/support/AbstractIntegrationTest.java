package uk.huy.pathwise.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests: full application context against MariaDB and Redis
 * Testcontainers (requires Docker), with emails captured by {@link FakeEmailSender} and geocoding answered by
 * {@link FakeGeocodingProvider}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, AbstractIntegrationTest.EmailTestConfiguration.class})
public abstract class AbstractIntegrationTest {
    @TestConfiguration(proxyBeanMethods = false)
    static class EmailTestConfiguration {
        @Bean
        @Primary
        FakeEmailSender fakeEmailSender() {
            return new FakeEmailSender();
        }

        @Bean
        @Primary
        FakeGeocodingProvider fakeGeocodingProvider() {
            return new FakeGeocodingProvider();
        }
    }
}
