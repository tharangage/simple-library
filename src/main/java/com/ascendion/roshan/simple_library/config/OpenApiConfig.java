package com.ascendion.roshan.simple_library.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Adds a Keycloak "Authorize" button to Swagger UI (authorization code flow with PKCE).
 * The URLs are opened by the user's browser, so they use the public Keycloak address.
 */
@Configuration
public class OpenApiConfig {

    private static final String SCHEME = "keycloak";

    @Bean
    public OpenAPI openAPI(@Value("${app.keycloak.public-url}") String keycloakUrl,
                           @Value("${app.keycloak.realm}") String realm) {
        String base = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect";
        SecurityScheme scheme = new SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(new OAuthFlows().authorizationCode(new OAuthFlow()
                        .authorizationUrl(base + "/auth")
                        .tokenUrl(base + "/token")
                        .scopes(new Scopes().addString("openid", "OpenID Connect"))));
        return new OpenAPI()
                .info(new Info().title("Simple Library API").version("v1"))
                .components(new Components().addSecuritySchemes(SCHEME, scheme))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME));
    }
}
