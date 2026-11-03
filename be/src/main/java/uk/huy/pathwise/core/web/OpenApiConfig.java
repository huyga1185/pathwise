package uk.huy.pathwise.core.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Header of the generated OpenAPI document ({@code /v3/api-docs}, dev and staging only), which clients use to generate
 * their API code (F-47). {@code docs/API_SPEC.md} stays the contract; {@code ApiSpecContractIntegrationTest} keeps both
 * in line.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {
    private static final String BEARER = "bearer";

    @Bean
    public OpenAPI pathwiseOpenApi() {
        return new OpenAPI()
                .info(new Info().title("PathWise API").version("v1")
                        .description("Contract: docs/API_SPEC.md. Errors: {code, details?}, API_SPEC §1."))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
