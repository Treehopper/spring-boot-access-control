package eu.hohenegger.accesscontrol.web;

import eu.hohenegger.accesscontrol.domain.Community;
import eu.hohenegger.accesscontrol.domain.User;
import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Lets controller methods declare a {@code User me} parameter for the chosen persona. */
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final Community community;

    CurrentUserArgumentResolver(Community community) {
        this.community = community;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == User.class;
    }

    @Override
    public User resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("No persona chosen");
        }
        return community.findUser(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("Unknown persona " + authentication.getName()));
    }
}
