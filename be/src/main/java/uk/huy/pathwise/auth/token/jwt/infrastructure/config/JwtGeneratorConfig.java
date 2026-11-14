package uk.huy.pathwise.auth.token.jwt.infrastructure.config;

import com.nimbusds.jose.JWSAlgorithm;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.huy.pathwise.auth.infrastructure.config.TokenProperties;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.JwtTokenGenerator;
import uk.huy.pathwise.auth.token.jwt.infrastructure.generator.nimbus.hmacgenerator.HMACJwtTokenGenerator;

@Configuration
@EnableConfigurationProperties(TokenProperties.class)
public class JwtGeneratorConfig {
    @Bean
    public JwtTokenGenerator hmacAccessTokenGenerator(TokenProperties properties) {
        TokenProperties.Jwt jwt = properties.jwt();
        return new HMACJwtTokenGenerator(JWSAlgorithm.parse(jwt.accessTokenHasingAlgorithm()),
                jwt.accessTokenSecret(),
                jwt.accessTokenExpirationTime(),
                jwt.accessTokenIssuer());
    }
}
