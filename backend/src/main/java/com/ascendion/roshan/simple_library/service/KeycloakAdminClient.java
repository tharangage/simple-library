package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.config.KeycloakAdminProperties;
import com.ascendion.roshan.simple_library.exception.EmailAlreadyRegisteredException;
import com.ascendion.roshan.simple_library.exception.KeycloakAdminException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Minimal Keycloak Admin REST client: create and delete users.
 * Signs in as the service account {@code library-backend} for each call (registration is low
 * volume, so there is no token cache to expire or leak). Passwords and mobile numbers are only
 * sent to Keycloak; they are never logged or put in exception messages.
 * The new user gets the realm's default roles, so the realm's default role must include {@code user}.
 */
@Component
public class KeycloakAdminClient {

    private static final ParameterizedTypeReference<Map<String, Object>> JSON_OBJECT =
            new ParameterizedTypeReference<>() { };

    /** What Keycloak needs to create an account. */
    public record NewUser(String email, String firstName, String lastName, String mobile, String password) {
        /** Never print the password or mobile number. */
        @Override
        public String toString() {
            return "NewUser[redacted]";
        }
    }

    private final RestClient restClient;
    private final KeycloakAdminProperties properties;

    public KeycloakAdminClient(final RestClient.Builder restClientBuilder, final KeycloakAdminProperties properties) {
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    /**
     * Creates an enabled user (email as username, not yet verified, permanent password).
     *
     * @return the new Keycloak user id (the future token {@code sub})
     * @throws EmailAlreadyRegisteredException if Keycloak already has a user with this email
     * @throws KeycloakAdminException          for any other failure
     */
    public String createUser(final NewUser newUser) {
        final String token = fetchServiceToken();
        final Map<String, Object> user = Map.of(
                "username", newUser.email(),
                "email", newUser.email(),
                "firstName", newUser.firstName(),
                "lastName", newUser.lastName(),
                "enabled", true,
                "emailVerified", false,
                "attributes", Map.of("mobile", List.of(newUser.mobile())),
                "credentials", List.of(Map.of("type", "password", "value", newUser.password(), "temporary", false)));
        try {
            final ResponseEntity<Void> response = restClient.post()
                    .uri("/admin/realms/{realm}/users", properties.realm())
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(user)
                    .retrieve()
                    .toBodilessEntity();
            return idFromLocation(response.getHeaders().getLocation());
        } catch (HttpClientErrorException.Conflict ex) {
            throw new EmailAlreadyRegisteredException(ex);
        } catch (RestClientException ex) {
            throw new KeycloakAdminException("Keycloak user creation failed", ex);
        }
    }

    /** Removes a user again (compensation when saving the borrower fails). */
    public void deleteUser(final String userId) {
        final String token = fetchServiceToken();
        try {
            restClient.delete()
                    .uri("/admin/realms/{realm}/users/{id}", properties.realm(), userId)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new KeycloakAdminException("Keycloak user deletion failed", ex);
        }
    }

    private String fetchServiceToken() {
        if (properties.clientSecret() == null || properties.clientSecret().isBlank()) {
            throw new KeycloakAdminException(
                    "Keycloak admin client secret is not configured (KEYCLOAK_ADMIN_CLIENT_SECRET)");
        }
        final MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        try {
            final Map<String, Object> body = restClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JSON_OBJECT);
            final Object token = body == null ? null : body.get("access_token");
            if (token == null) {
                throw new KeycloakAdminException("Keycloak returned no access token for the service account");
            }
            return token.toString();
        } catch (RestClientException ex) {
            throw new KeycloakAdminException("Could not get a Keycloak service-account token", ex);
        }
    }

    private static String idFromLocation(final URI location) {
        if (location == null) {
            throw new KeycloakAdminException("Keycloak did not return the new user's location");
        }
        final String path = location.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
