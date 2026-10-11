package com.ascendion.roshan.simple_library.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserTest {

    private static CurrentUser user(final String... authorities) {
        final Jwt jwt = new Jwt("t", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), Map.of("sub", "kc-1"));
        return new CurrentUser(jwt, Set.of(authorities));
    }

    @Test
    void roles_userAndAdmin_listedInFixedOrder() {
        assertThat(user("ROLE_ADMIN", "ROLE_USER").roles()).containsExactly("user", "admin");
    }

    @Test
    void roles_otherKeycloakRoles_areLeftOut() {
        assertThat(user("ROLE_ADMIN", "ROLE_OFFLINE_ACCESS", "ROLE_DEFAULT-ROLES-LIBRARY").roles()).containsExactly("admin");
    }

    @Test
    void roles_none_isEmpty() {
        assertThat(user("SCOPE_read").roles()).isEmpty();
    }
}
