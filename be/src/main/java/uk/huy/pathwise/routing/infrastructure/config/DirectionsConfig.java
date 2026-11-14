package uk.huy.pathwise.routing.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.core.observability.FailureLog;
import uk.huy.pathwise.routing.infrastructure.failure.FailureLoggingDirectionsProvider;
import uk.huy.pathwise.routing.directions.DirectionsProvider;
import uk.huy.pathwise.routing.infrastructure.osrm.OsrmDirectionsProvider;

/**
 * The single {@link DirectionsProvider} bean, chosen by {@code routing.directions.provider} (ARCHITECTURE.md §8.5).
 */
@Configuration
@EnableConfigurationProperties(DirectionsProperties.class)
public class DirectionsConfig {
    @Bean
    DirectionsProvider directionsProvider(DirectionsProperties properties, RestClient.Builder restClientBuilder,
                                          FailureLog failureLog) {
        DirectionsProvider provider = switch (properties.provider()) {
            case "osrm" -> osrm(properties.osrm(), restClientBuilder);
            default -> throw new IllegalStateException("Unknown routing.directions.provider: " + properties.provider());
        };
        return new FailureLoggingDirectionsProvider(provider, properties.provider(), failureLog);
    }

    private static DirectionsProvider osrm(DirectionsProperties.Osrm osrm, RestClient.Builder restClientBuilder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(osrm.connectTimeout());
        requestFactory.setReadTimeout(osrm.readTimeout());
        RestClient restClient = restClientBuilder.clone()
                .baseUrl(osrm.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", osrm.userAgent())
                // The public OSRM server sends compressed bodies the simple client cannot read.
                .defaultHeader("Accept-Encoding", "identity")
                .build();
        return new OsrmDirectionsProvider(restClient, osrm.profile());
    }
}
