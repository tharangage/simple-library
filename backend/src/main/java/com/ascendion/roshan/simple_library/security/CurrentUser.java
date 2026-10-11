package com.ascendion.roshan.simple_library.security;

import com.ascendion.roshan.simple_library.config.SecurityConfig;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
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

    /** The library roles of the caller, lower case and in a fixed order: {@code user}, {@code admin}. Other Keycloak roles are left out. */
    public List<String> roles() {
        final List<String> roles = new java.util.ArrayList<>();
        if (isUser()) {
            roles.add("user");
        }
        if (isAdmin()) {
            roles.add("admin");
        }
        return List.copyOf(roles);
    }
}
