package uk.huy.pathwise.auth.infrastructure.config;

import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import uk.huy.pathwise.auth.model.ClientDetail;

/**
 * Keeps {@link ClientDetail} out of the generated OpenAPI document (F-47): it is filled from the request by
 * {@code ClientDetailResolver}, clients never send it, but springdoc would otherwise list it as a query parameter.
 */
@Configuration(proxyBeanMethods = false)
public class AuthOpenApiConfig {
    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(ClientDetail.class);
    }
}
