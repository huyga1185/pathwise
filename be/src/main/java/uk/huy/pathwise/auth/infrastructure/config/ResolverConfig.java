package uk.huy.pathwise.auth.infrastructure.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.huy.pathwise.auth.infrastructure.resolver.ClientDetailResolver;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class ResolverConfig implements WebMvcConfigurer {
    private final ClientDetailResolver clientDetailResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(clientDetailResolver);
    }
}
