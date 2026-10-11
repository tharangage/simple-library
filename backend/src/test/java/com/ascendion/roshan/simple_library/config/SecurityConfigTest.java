package com.ascendion.roshan.simple_library.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    private AbstractAuthenticationToken convert(final Map<String, Object> claims) {
        final Map<String, Object> all = new java.util.HashMap<>(claims);
        all.put("sub", "kc-1");
        final Jwt jwt = new Jwt("t", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "none"), all);
        return securityConfig.jwtAuthenticationConverter().convert(jwt);
    }

    @Test
    void converter_realmRoles_becomeUpperCaseRoleAuthorities() {
        final AbstractAuthenticationToken token = convert(Map.of("realm_access", Map.of("roles", List.of("user", "admin", "offline_access"))));

        assertThat(token.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_OFFLINE_ACCESS");
    }

    @Test
    void converter_tokenWithoutRoles_hasNoAuthorities() {
        assertThat(convert(Map.of()).getAuthorities()).isEmpty();
    }

    @Test
    void converter_malformedRealmAccess_hasNoAuthorities() {
        assertThat(convert(Map.of("realm_access", "oops")).getAuthorities()).isEmpty();
    }
}
