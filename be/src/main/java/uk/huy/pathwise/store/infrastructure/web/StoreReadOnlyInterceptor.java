package uk.huy.pathwise.store.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.huy.pathwise.core.web.AllowedWhenStoreReadOnly;
import uk.huy.pathwise.shared.identity.AuthenticatedUser;
import uk.huy.pathwise.store.service.StoreLockService;

import java.util.Set;

/**
 * The one place where writes of a locked store are refused (T56, P-62): every POST, PUT, PATCH or DELETE of a store
 * member whose store is READ_ONLY gets {@code STORE_READ_ONLY} (423), unless the handler is marked
 * {@link AllowedWhenStoreReadOnly}. Blocked by default, so a new write endpoint cannot be forgotten. Requests without
 * a store (public, ADMIN) pass; the public invitation accept checks the store itself.
 */
@RequiredArgsConstructor
public class StoreReadOnlyInterceptor implements HandlerInterceptor {
    private static final Set<String> READS = Set.of("GET", "HEAD", "OPTIONS");

    private final StoreLockService storeLockService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (READS.contains(request.getMethod()) || !(handler instanceof HandlerMethod method)) return true;
        if (AnnotatedElementUtils.hasAnnotation(method.getMethod(), AllowedWhenStoreReadOnly.class)
                || AnnotatedElementUtils.hasAnnotation(method.getBeanType(), AllowedWhenStoreReadOnly.class)) return true;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) return true;
        Long storeId = user.identity().storeId();
        if (storeId != null) storeLockService.checkWritable(storeId);
        return true;
    }
}
