package com.ascendion.roshan.simple_library.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless resource server: every API call needs a Keycloak-issued bearer token.
 * Swagger UI itself, the OpenAPI document, the H2 console and {@code POST /apis/v1/registrations}
 * stay open so the login flow can start and visitors can sign up.
 */
@Configuration
public class SecurityConfig {

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
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
        return http.build();
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
