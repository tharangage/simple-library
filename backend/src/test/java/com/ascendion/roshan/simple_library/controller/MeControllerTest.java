package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.config.SecurityConfig;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MeController.class)
@Import(SecurityConfig.class)
class MeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowerProvisioningService provisioningService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void me_noToken_returns401() throws Exception {
        mockMvc.perform(get("/apis/v1/me")).andExpect(status().isUnauthorized());

        verifyNoInteractions(provisioningService);
    }

    @Test
    void me_validToken_returnsBorrower() throws Exception {
        final Borrower borrower = new Borrower();
        borrower.setId("b-1");
        borrower.setFirstname("Ann");
        borrower.setEmail("ann@example.com");
        when(provisioningService.getOrCreate(any(Jwt.class))).thenReturn(borrower);

        mockMvc.perform(get("/apis/v1/me").with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b-1"))
                .andExpect(jsonPath("$.firstname").value("Ann"));
    }

    @Test
    void me_emailConflict_returns409() throws Exception {
        when(provisioningService.getOrCreate(any(Jwt.class)))
                .thenThrow(new IllegalStateException("Email matches an existing borrower but is not verified"));

        mockMvc.perform(get("/apis/v1/me").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(content().string("Email matches an existing borrower but is not verified"));
    }
}
