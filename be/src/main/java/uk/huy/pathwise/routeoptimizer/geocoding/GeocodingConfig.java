package uk.huy.pathwise.routeoptimizer.geocoding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GeocodingConfig {
    @Bean
    RestClient nominatimRestClient(
            RestClient.Builder builder,
            @Value("${api.geocoding.nominatim-base-url}") String nominatimBaseUrl,
            @Value("${api.user-agent}") String userAgent) {
        return builder
                .clone()
                .baseUrl(nominatimBaseUrl)
                .defaultHeader("User-Agent", userAgent)
                .requestInterceptor((request, body, execution) -> {
                    System.out.println(">>> Geocoding URI: " + request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }
}
