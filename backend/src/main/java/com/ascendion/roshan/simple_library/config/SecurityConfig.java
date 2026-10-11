package com.ascendion.roshan.simple_library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stateless resource server: every API call needs a Keycloak-issued bearer token.
 * Swagger UI itself, the OpenAPI document, the H2 console and {@code POST /apis/v1/registrations}
 * stay open so the login flow can start and visitors can sign up.
 */
@Configuration
public class SecurityConfig {

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        http
                // Bearer tokens only, no cookies or sessions, so CSRF protection is not needed.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())   // uses the corsConfigurationSource bean below
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Self-registration: a visitor has no account, so no token yet.
                        .requestMatchers(HttpMethod.POST, "/apis/v1/registrations").permitAll()
                        // Returning a book is a library-desk action: users cannot return books themselves.
                        .requestMatchers(HttpMethod.DELETE, "/apis/v1/borrowers/*/books/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/apis/v1/borrowers/*/books").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/apis/v1/borrowers/me").hasRole("USER")
                        .requestMatchers("/apis/v1/me", "/apis/v1/me/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Maps the Keycloak realm roles in {@code realm_access.roles} to {@code ROLE_USER} / {@code ROLE_ADMIN}. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        final Converter<Jwt, Collection<GrantedAuthority>> roles = jwt -> {
            final Object realmAccess = jwt.getClaim("realm_access");
            if (realmAccess instanceof Map<?, ?> map && map.get("roles") instanceof Collection<?> names) {
                return names.stream()
                        .map(name -> (GrantedAuthority) new SimpleGrantedAuthority(
                                "ROLE_" + String.valueOf(name).toUpperCase(Locale.ROOT)))
                        .toList();
            }
            return List.of();
        };
        final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }

    /**
     * Lets the React front end (served from another origin) call the API with a bearer token.
     * Configure with {@code app.cors.allowed-origins} (comma-separated).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}") final List<String> allowedOrigins) {
        final CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/apis/**", config);
        return source;
    }
}
