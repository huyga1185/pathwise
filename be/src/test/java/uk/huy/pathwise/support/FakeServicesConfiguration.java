package uk.huy.pathwise.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * The fakes of the integration tests (email, geocoding, matrix, directions). Its own top-level class, imported by
 * {@link AbstractIntegrationTest}: Spring Framework 7.1 stops ignoring a nested configuration class that is also
 * imported, which would register these beans twice (F-35).
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakeServicesConfiguration {
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

    @Bean
    @Primary
    FakeMatrixProvider fakeMatrixProvider() {
        return new FakeMatrixProvider();
    }

    @Bean
    @Primary
    FakeDirectionsProvider fakeDirectionsProvider() {
        return new FakeDirectionsProvider();
    }
}
