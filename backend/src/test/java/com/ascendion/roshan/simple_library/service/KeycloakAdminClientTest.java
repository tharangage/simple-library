package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.config.KeycloakAdminProperties;
import com.ascendion.roshan.simple_library.exception.EmailAlreadyRegisteredException;
import com.ascendion.roshan.simple_library.exception.KeycloakAdminException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** No real Keycloak: the HTTP calls are answered by MockRestServiceServer. */
class KeycloakAdminClientTest {

    private static final String BASE = "http://keycloak.test";
    private static final String TOKEN_URL = BASE + "/realms/library/protocol/openid-connect/token";
    private static final String USERS_URL = BASE + "/admin/realms/library/users";

    private MockRestServiceServer server;
    private KeycloakAdminClient client;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakAdminClient(builder,
                new KeycloakAdminProperties(BASE, "library", "library-backend", "the-secret"));
    }

    private void expectToken() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("client_id=library-backend")))
                .andRespond(withSuccess("{\"access_token\":\"svc-token\"}", MediaType.APPLICATION_JSON));
    }

    @Test
    void createUser_success_postsUserWithPasswordAndReturnsIdFromLocation() {
        expectToken();
        server.expect(requestTo(USERS_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer svc-token"))
                .andExpect(jsonPath("$.username").value("ann@example.com"))
                .andExpect(jsonPath("$.email").value("ann@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ann"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.emailVerified").value(false))
                .andExpect(jsonPath("$.attributes.mobile[0]").value("+14155550100"))
                .andExpect(jsonPath("$.credentials[0].type").value("password"))
                .andExpect(jsonPath("$.credentials[0].value").value("s3cret-pass"))
                .andExpect(jsonPath("$.credentials[0].temporary").value(false))
                .andRespond(withStatus(HttpStatus.CREATED)
                        .location(java.net.URI.create(USERS_URL + "/kc-123")));

        final String id = client.createUser(new KeycloakAdminClient.NewUser("ann@example.com", "Ann", "Lee", "+14155550100", "s3cret-pass"));

        assertThat(id).isEqualTo("kc-123");
        server.verify();
    }

    @Test
    void createUser_keycloakAnswers409_throwsEmailAlreadyRegistered() {
        expectToken();
        server.expect(requestTo(USERS_URL)).andRespond(withStatus(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> client.createUser(new KeycloakAdminClient.NewUser("ann@example.com", "Ann", "Lee", "+14155550100", "s3cret-pass")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void createUser_keycloakAnswers500_throwsKeycloakAdminExceptionWithoutUserData() {
        expectToken();
        server.expect(requestTo(USERS_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.createUser(new KeycloakAdminClient.NewUser("ann@example.com", "Ann", "Lee", "+14155550100", "s3cret-pass")))
                .isInstanceOf(KeycloakAdminException.class)
                .hasMessageNotContaining("s3cret-pass")
                .hasMessageNotContaining("ann@example.com");
    }

    @Test
    void createUser_noLocationHeader_throwsKeycloakAdminException() {
        expectToken();
        server.expect(requestTo(USERS_URL)).andRespond(withStatus(HttpStatus.CREATED));

        assertThatThrownBy(() -> client.createUser(new KeycloakAdminClient.NewUser("a@b.co", "A", "B", "+14155550100", "s3cret-pass")))
                .isInstanceOf(KeycloakAdminException.class);
    }

    @Test
    void createUser_tokenRequestRejected_throwsKeycloakAdminException() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.createUser(new KeycloakAdminClient.NewUser("a@b.co", "A", "B", "+14155550100", "s3cret-pass")))
                .isInstanceOf(KeycloakAdminException.class);
    }

    @Test
    void createUser_tokenResponseWithoutAccessToken_throwsKeycloakAdminException() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createUser(new KeycloakAdminClient.NewUser("a@b.co", "A", "B", "+14155550100", "s3cret-pass")))
                .isInstanceOf(KeycloakAdminException.class);
    }

    @Test
    void createUser_secretNotConfigured_failsWithoutCallingKeycloak() {
        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer none = MockRestServiceServer.bindTo(builder).build();
        final KeycloakAdminClient unconfigured = new KeycloakAdminClient(builder,
                new KeycloakAdminProperties(BASE, "library", "library-backend", ""));

        assertThatThrownBy(() -> unconfigured.createUser(new KeycloakAdminClient.NewUser("a@b.co", "A", "B", "+14155550100", "s3cret-pass")))
                .isInstanceOf(KeycloakAdminException.class)
                .hasMessageContaining("KEYCLOAK_ADMIN_CLIENT_SECRET");
        none.verify();
    }

    @Test
    void deleteUser_success_callsDeleteWithServiceToken() {
        expectToken();
        server.expect(requestTo(USERS_URL + "/kc-123"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer svc-token"))
                .andRespond(withNoContent());

        client.deleteUser("kc-123");

        server.verify();
    }

    @Test
    void deleteUser_keycloakFails_throwsKeycloakAdminException() {
        expectToken();
        server.expect(requestTo(USERS_URL + "/kc-123")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.deleteUser("kc-123")).isInstanceOf(KeycloakAdminException.class);
    }

    @Test
    void propertiesToString_hidesTheSecret() {
        assertThat(new KeycloakAdminProperties(BASE, "library", "library-backend", "the-secret").toString())
                .doesNotContain("the-secret");
    }
}
