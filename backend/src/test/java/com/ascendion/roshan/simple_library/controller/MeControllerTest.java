package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.config.SecurityConfig;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.service.BorrowedBookService;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

    private static final SimpleGrantedAuthority USER = new SimpleGrantedAuthority("ROLE_USER");
    private static final SimpleGrantedAuthority ADMIN = new SimpleGrantedAuthority("ROLE_ADMIN");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowerProvisioningService provisioningService;

    @MockitoBean
    private BorrowedBookService borrowedBookService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static Borrower borrower() {
        final Borrower borrower = new Borrower();
        borrower.setId("b-1");
        borrower.setFirstname("Ann");
        borrower.setMobile("+31612345678");
        return borrower;
    }

    @Test
    void me_noToken_returns401() throws Exception {
        mockMvc.perform(get("/apis/v1/me")).andExpect(status().isUnauthorized());

        verifyNoInteractions(provisioningService);
    }

    @Test
    void me_noRole_returns403() throws Exception {
        mockMvc.perform(get("/apis/v1/me").with(jwt())).andExpect(status().isForbidden());

        verifyNoInteractions(provisioningService);
    }

    @Test
    void me_user_returnsTokenProfileWithBorrowerSection() throws Exception {
        when(provisioningService.getOrCreate(any(Jwt.class))).thenReturn(borrower());
        when(borrowedBookService.borrowedCount("b-1")).thenReturn(2L);
        when(borrowedBookService.maxItemsPerBorrower()).thenReturn(3);

        mockMvc.perform(get("/apis/v1/me").with(jwt().authorities(USER)
                        .jwt(j -> j.subject("kc-1").claim("given_name", "Ann").claim("family_name", "Lee")
                                .claim("email", "ann@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("kc-1"))
                .andExpect(jsonPath("$.firstname").value("Ann"))
                .andExpect(jsonPath("$.lastname").value("Lee"))
                .andExpect(jsonPath("$.email").value("ann@example.com"))
                .andExpect(jsonPath("$.borrower.id").value("b-1"))
                .andExpect(jsonPath("$.mobile").value("+31612345678"))
                .andExpect(jsonPath("$.borrower.borrowedCount").value(2))
                .andExpect(jsonPath("$.borrower.maxItems").value(3))
                .andExpect(jsonPath("$.borrower.remainingSlots").value(1))
                // The name, email and mobile live once, at the top level; the borrower section does not repeat them.
                .andExpect(jsonPath("$.borrower.mobile").doesNotExist())
                .andExpect(jsonPath("$.borrower.firstname").doesNotExist())
                .andExpect(jsonPath("$.borrower.lastname").doesNotExist())
                .andExpect(jsonPath("$.borrower.email").doesNotExist());
    }

    @Test
    void me_userAlsoAdmin_hasABorrowerSection() throws Exception {
        when(provisioningService.getOrCreate(any(Jwt.class))).thenReturn(borrower());

        mockMvc.perform(get("/apis/v1/me").with(jwt().authorities(USER, ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("user"))
                .andExpect(jsonPath("$.roles[1]").value("admin"))
                .andExpect(jsonPath("$.borrower.id").value("b-1"));
    }

    @Test
    void me_adminOnly_hasNoBorrowerAndIsNotProvisioned() throws Exception {
        mockMvc.perform(get("/apis/v1/me")
                        .with(jwt().authorities(ADMIN).jwt(j -> j.subject("kc-2").claim("given_name", "Ada")
                                .claim("email", "ada@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("kc-2"))
                .andExpect(jsonPath("$.firstname").value("Ada"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("admin"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.mobile").doesNotExist())
                .andExpect(jsonPath("$.borrower").doesNotExist());

        verifyNoInteractions(provisioningService);
    }

    @Test
    void me_user_createsTheBorrowerThroughTheService() throws Exception {
        when(provisioningService.getOrCreate(any(Jwt.class))).thenReturn(borrower());

        mockMvc.perform(get("/apis/v1/me").with(jwt().authorities(USER))).andExpect(status().isOk());

        verify(provisioningService).getOrCreate(any(Jwt.class));
    }

    @Test
    void me_emailConflict_returns409() throws Exception {
        when(provisioningService.getOrCreate(any(Jwt.class)))
                .thenThrow(new IllegalStateException("Email matches an existing borrower but is not verified"));

        mockMvc.perform(get("/apis/v1/me").with(jwt().authorities(USER)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Email matches an existing borrower but is not verified"));
    }
}
