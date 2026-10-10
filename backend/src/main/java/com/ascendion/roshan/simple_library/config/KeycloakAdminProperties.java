package com.ascendion.roshan.simple_library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * How the backend reaches the Keycloak Admin REST API to create users (self-registration).
 * It signs in as the confidential client {@code library-backend} (service account) with the client
 * credentials grant. The secret comes from {@code KEYCLOAK_ADMIN_CLIENT_SECRET}; there is no default.
 *
 * @param baseUrl      Keycloak URL as seen from the backend (inside Docker this is not localhost)
 * @param realm        realm that holds the users
 * @param clientId     service-account client; needs only realm-management manage-users and view-users
 * @param clientSecret secret of that client (empty = registration is not configured)
 */
@ConfigurationProperties(prefix = "app.keycloak.admin")
public record KeycloakAdminProperties(String baseUrl, String realm, String clientId, String clientSecret) {

    /** Hides the secret if the object is ever logged. */
    @Override
    public String toString() {
        return "KeycloakAdminProperties[baseUrl=" + baseUrl + ", realm=" + realm + ", clientId=" + clientId + "]";
    }
}
