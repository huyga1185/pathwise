package uk.huy.pathwise.routeoptimizer.infrastructure.matrix.osrmprovider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OSRMMatrixProviderConfig {
    @Bean
    RestClient osrmRestClient(
            RestClient.Builder builder,
            @Value("${api.matrix.osrm-base-url}") String osrmBaseUrl,
            @Value("${api.user-agent}") String userAgent) {
        return builder
                .clone()
                .baseUrl(osrmBaseUrl)
                .defaultHeader("User-Agent", userAgent)
                .defaultHeader("Accept-Encoding", "identity")
                .requestInterceptor((request, body, execution) -> {
                    System.out.println(">>> OSRM URI: " + request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }
}
