package com.ascendion.roshan.simple_library.security;

import com.ascendion.roshan.simple_library.config.SecurityConfig;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

/**
 * Who is calling: the validated token and the authorities Spring Security derived from it
 * ({@code ROLE_USER}, {@code ROLE_ADMIN}). Read it with {@link CurrentUserHolder#get()}.
 */
public record CurrentUser(Jwt jwt, Set<String> authorities) {

    /** The Keycloak user id ({@code sub}). */
    public String subject() {
        return jwt.getSubject();
    }

    public boolean isUser() {
        return authorities.contains(SecurityConfig.ROLE_USER);
    }

    public boolean isAdmin() {
        return authorities.contains(SecurityConfig.ROLE_ADMIN);
    }
}
