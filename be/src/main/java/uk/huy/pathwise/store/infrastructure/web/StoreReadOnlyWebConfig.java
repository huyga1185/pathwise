package uk.huy.pathwise.store.infrastructure.web;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.huy.pathwise.store.service.StoreLockService;

/**
 * Registers {@link StoreReadOnlyInterceptor} on the API (T56). In a {@code @WebMvcTest} slice there is no
 * {@link StoreLockService}, so nothing is registered there.
 */
@Configuration
@RequiredArgsConstructor
public class StoreReadOnlyWebConfig implements WebMvcConfigurer {
    private final ObjectProvider<StoreLockService> storeLockService;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        StoreLockService service = storeLockService.getIfAvailable();
        if (service != null) registry.addInterceptor(new StoreReadOnlyInterceptor(service)).addPathPatterns("/api/**");
    }
}
