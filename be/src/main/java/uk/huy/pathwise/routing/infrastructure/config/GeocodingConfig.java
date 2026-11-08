package uk.huy.pathwise.routing.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import uk.huy.pathwise.core.security.Hasher;
import uk.huy.pathwise.routing.geocoding.CachingGeocodingProvider;
import uk.huy.pathwise.routing.geocoding.GeocodingProvider;
import uk.huy.pathwise.routing.infrastructure.cache.GeocodingCacheStore;
import uk.huy.pathwise.routing.infrastructure.nominatim.NominatimGeocodingProvider;
import uk.huy.pathwise.routing.infrastructure.throttle.RedisRequestThrottle;
import uk.huy.pathwise.routing.infrastructure.throttle.RequestThrottle;

/**
 * The single {@link GeocodingProvider} bean: the provider chosen by {@code routing.geocoding.provider}, wrapped in the
 * cache (ARCHITECTURE.md §8.5: choose the implementation by property, not by {@code @Qualifier}).
 */
@Configuration
@EnableConfigurationProperties(GeocodingProperties.class)
public class GeocodingConfig {
    static final String NOMINATIM_THROTTLE_KEY = "geocode:nominatim:next-slot";

    @Bean
    GeocodingProvider geocodingProvider(GeocodingProperties properties,
                                        RestClient.Builder restClientBuilder,
                                        StringRedisTemplate redisTemplate,
                                        GeocodingCacheStore cache,
                                        Hasher hasher,
                                        ObjectMapper objectMapper) {
        GeocodingProvider upstream = switch (properties.provider()) {
            case "nominatim" -> nominatim(properties.nominatim(), restClientBuilder, redisTemplate);
            default -> throw new IllegalStateException("Unknown routing.geocoding.provider: " + properties.provider());
        };
        return new CachingGeocodingProvider(upstream, properties.provider(), cache, hasher, objectMapper, properties.cacheTtl());
    }

    private static GeocodingProvider nominatim(GeocodingProperties.Nominatim nominatim,
                                               RestClient.Builder restClientBuilder,
                                               StringRedisTemplate redisTemplate) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(nominatim.connectTimeout());
        requestFactory.setReadTimeout(nominatim.readTimeout());
        RestClient restClient = restClientBuilder.clone()
                .baseUrl(nominatim.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", nominatim.userAgent())
                .build();
        GeocodingProperties.RateLimit rateLimit = nominatim.rateLimit();
        RequestThrottle throttle = rateLimit.enabled()
                ? new RedisRequestThrottle(redisTemplate, NOMINATIM_THROTTLE_KEY, rateLimit.perSecond(), rateLimit.maxWait())
                : () -> true;
        return new NominatimGeocodingProvider(restClient, throttle);
    }
}
