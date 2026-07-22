package uk.huy.pathwise.auth.infastructure.resolver;

import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import uk.huy.pathwise.auth.infastructure.annotation.CurrentClient;
import uk.huy.pathwise.auth.model.ClientDetail;

@Component
public class ClientDetailResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentClient.class) &&
                parameter.getParameterType().equals(ClientDetail.class);
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter,
                                            @Nullable ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest,
                                            @Nullable WebDataBinderFactory binderFactory) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) throw new IllegalStateException("No authentication in security context");
        return authentication.getDetails() instanceof ClientDetail clientDetail ? clientDetail : null;
    }
}
