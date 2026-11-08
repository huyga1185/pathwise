package uk.huy.pathwise.routing.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import uk.huy.pathwise.routing.infrastructure.osrm.OsrmMatrixProvider;
import uk.huy.pathwise.routing.matrix.MatrixProvider;

/**
 * The single {@link MatrixProvider} bean, chosen by {@code routing.matrix.provider} (ARCHITECTURE.md §8.5).
 */
@Configuration
@EnableConfigurationProperties(MatrixProperties.class)
public class MatrixConfig {
    @Bean
    MatrixProvider matrixProvider(MatrixProperties properties, RestClient.Builder restClientBuilder) {
        return switch (properties.provider()) {
            case "osrm" -> osrm(properties.osrm(), restClientBuilder);
            default -> throw new IllegalStateException("Unknown routing.matrix.provider: " + properties.provider());
        };
    }

    private static MatrixProvider osrm(MatrixProperties.Osrm osrm, RestClient.Builder restClientBuilder) {
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
        return new OsrmMatrixProvider(restClient, osrm.profile(), osrm.maxTableSize());
    }
}
