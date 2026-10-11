package com.ascendion.roshan.simple_library.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.stream.Collectors;

/**
 * Puts the token details of the caller into {@link CurrentUserHolder} for every API call that has a valid
 * token, and removes them when the request is finished. Requests without a token (self-registration, Swagger)
 * leave the holder empty. Registered for all {@code /apis/**} paths in {@code WebConfig}, before the other interceptors.
 */
public class CurrentUserInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        final var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            CurrentUserHolder.set(new CurrentUser(jwt, authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toUnmodifiableSet())));
        }
        return true;
    }

    /** Runs after success or failure, so the thread never keeps the previous caller. */
    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response,
                                final Object handler, final Exception ex) {
        CurrentUserHolder.clear();
    }
}
