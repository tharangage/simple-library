package com.ascendion.roshan.simple_library.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserInterceptorTest {

    private final CurrentUserInterceptor interceptor = new CurrentUserInterceptor();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        CurrentUserHolder.clear();
    }

    private static Jwt jwt() {
        return new Jwt("t", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), Map.of("sub", "kc-1"));
    }

    @Test
    void preHandle_tokenWithRoles_fillsTheHolder() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThat(interceptor.preHandle(request, response, new Object())).isTrue();

        final CurrentUser user = CurrentUserHolder.require();
        assertThat(user.subject()).isEqualTo("kc-1");
        assertThat(user.isUser()).isTrue();
        assertThat(user.isAdmin()).isTrue();
    }

    @Test
    void preHandle_adminOnlyToken_isNotAUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(), List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        interceptor.preHandle(request, response, new Object());

        assertThat(CurrentUserHolder.require().isUser()).isFalse();
    }

    @Test
    void preHandle_noAuthentication_leavesTheHolderEmpty() {
        interceptor.preHandle(request, response, new Object());

        assertThat(CurrentUserHolder.get()).isEmpty();
        assertThatThrownBy(CurrentUserHolder::require).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void preHandle_authenticationWithoutJwt_leavesTheHolderEmpty() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("someone", "x"));

        interceptor.preHandle(request, response, new Object());

        assertThat(CurrentUserHolder.get()).isEmpty();
    }

    @Test
    void afterCompletion_clearsTheHolderEvenAfterAnError() {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt(), List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        interceptor.preHandle(request, response, new Object());

        interceptor.afterCompletion(request, response, new Object(), new RuntimeException("boom"));

        assertThat(CurrentUserHolder.get()).isEmpty();
    }
}
