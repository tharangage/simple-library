package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.config.SecurityConfig;
import com.ascendion.roshan.simple_library.dto.RegistrationRequest;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.exception.EmailAlreadyRegisteredException;
import com.ascendion.roshan.simple_library.exception.KeycloakAdminException;
import com.ascendion.roshan.simple_library.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
class RegistrationControllerTest {

    private static final String URL = "/apis/v1/registrations";
    private static final String VALID_BODY = """
            {"firstName":"Ann","lastName":"Lee","email":"ann@example.com",
             "mobile":"+14155550100","password":"s3cret-pass"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void register_validRequestWithoutToken_returns201WithLocationAndNoSecrets() throws Exception {
        final Borrower saved = new Borrower();
        saved.setId("b-1");
        saved.setFirstname("Ann");
        saved.setLastname("Lee");
        saved.setEmail("ann@example.com");
        saved.setMobile("+14155550100");
        when(registrationService.register(any(RegistrationRequest.class))).thenReturn(saved);

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/apis/v1/me"))
                .andExpect(jsonPath("$.id").value("b-1"))
                .andExpect(jsonPath("$.firstName").value("Ann"))
                .andExpect(jsonPath("$.email").value("ann@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.mobile").doesNotExist());
    }

    @Test
    void register_emptyBody_returns400WithAFieldErrorForEveryField() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.firstName").exists())
                .andExpect(jsonPath("$.lastName").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.mobile").exists())
                .andExpect(jsonPath("$.password").exists());

        verifyNoInteractions(registrationService);
    }

    @Test
    void register_badEmailMobileAndShortPassword_returns400WithMessages() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Ann","lastName":"Lee","email":"ann@example",
                         "mobile":"0415 555 0100","password":"short"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email must be valid"))
                .andExpect(jsonPath("$.mobile").exists())
                .andExpect(jsonPath("$.password").value("Password must be 8 to 128 characters"))
                .andExpect(jsonPath("$.firstName").doesNotExist());

        verifyNoInteractions(registrationService);
    }

    @Test
    void register_emailAlreadyRegistered_returns409WithCode() throws Exception {
        when(registrationService.register(any(RegistrationRequest.class)))
                .thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().string("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void register_keycloakFailure_returns500WithoutDetails() throws Exception {
        when(registrationService.register(any(RegistrationRequest.class)))
                .thenThrow(new KeycloakAdminException("Keycloak user creation failed"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal server error"));
    }

    @Test
    void otherEndpoints_stillNeedAToken() throws Exception {
        mockMvc.perform(post("/apis/v1/registrations/other").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
