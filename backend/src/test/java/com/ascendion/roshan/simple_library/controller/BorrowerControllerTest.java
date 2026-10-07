package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.config.SecurityConfig;
import com.ascendion.roshan.simple_library.dto.BorrowerCreateRequest;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.service.BorrowerService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BorrowerController.class)
@Import(SecurityConfig.class)
class BorrowerControllerTest {

    private static final String URL = "/apis/v1/borrowers";
    private static final String VALID_BODY = """
            {"firstname": "Ann", "lastname": "Lee", "email": "ann@example.com"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowerService borrowerService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void registerBorrower_noToken_returns401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(borrowerService);
    }

    @Test
    void registerBorrower_validRequest_returns200WithBorrowerJson() throws Exception {
        final Borrower saved = new Borrower();
        saved.setId("b-1");
        saved.setFirstname("Ann");
        saved.setLastname("Lee");
        saved.setEmail("ann@example.com");
        when(borrowerService.registerBorrower(any(BorrowerCreateRequest.class))).thenReturn(saved);

        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())   // known issue: 200, not 201 like books
                .andExpect(jsonPath("$.id").value("b-1"))
                .andExpect(jsonPath("$.firstname").value("Ann"))
                .andExpect(jsonPath("$.lastname").value("Lee"))
                .andExpect(jsonPath("$.email").value("ann@example.com"));
    }

    @Test
    void registerBorrower_invalidEmailAndBlankName_returns400WithFieldMap() throws Exception {
        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstname": "", "lastname": "Lee", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email must be valid"))
                .andExpect(jsonPath("$.firstname").exists());

        verifyNoInteractions(borrowerService);
    }

    @Test
    void registerBorrower_duplicateEmail_returns409WithMessage() throws Exception {
        when(borrowerService.registerBorrower(any(BorrowerCreateRequest.class)))
                .thenThrow(new IllegalStateException("Email already exists"));

        mockMvc.perform(post(URL).with(jwt()).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().string("Email already exists"));
    }
}
