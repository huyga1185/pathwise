package uk.huy.pathwise.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests: full application context against MariaDB and Redis
 * Testcontainers (requires Docker), with emails captured by {@link FakeEmailSender} and geocoding and travel matrices
 * answered by {@link FakeGeocodingProvider}, {@link FakeMatrixProvider} and {@link FakeDirectionsProvider}
 * ({@link FakeServicesConfiguration}).
 */
@SpringBootTest
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, FakeServicesConfiguration.class})
public abstract class AbstractIntegrationTest {}
