package com.ascendion.roshan.simple_library.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetBooksApi() throws Exception {
        mockMvc.perform(get("/apis/v1/books").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    public void testRegisterBooksApi() throws Exception {
        mockMvc.perform(post("/apis/v1/books").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "isbn": "978-955-675-510-7",
                          "title": "string",
                          "author": "string"
                        }
                        """))
                .andExpect(status().isCreated());
    }

    @Test
    void listBooks_noToken_returns401() throws Exception {
        mockMvc.perform(get("/apis/v1/books"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerBook_isbn10EndingInX_returns201WithBookJson() throws Exception {
        mockMvc.perform(post("/apis/v1/books").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"isbn": "0-8044-2957-X", "title": "Integration Title", "author": "Integration Author"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.isbn").value("0-8044-2957-X"))
                .andExpect(jsonPath("$.borrowed").value(false))
                .andExpect(jsonPath("$.borrowedBy").doesNotExist());
    }

    @Test
    void registerBook_invalidIsbn_returns400WithFieldMap() throws Exception {
        mockMvc.perform(post("/apis/v1/books").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"isbn": "12345", "title": "T", "author": "A"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isbn").exists());
    }

    @Test
    void registerBook_isbnLongerThan17Characters_returns400() throws Exception {
        mockMvc.perform(post("/apis/v1/books").with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"isbn": "----------0134685997", "title": "T", "author": "A"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isbn").value("ISBN must be between 10 and 17 characters"));
    }

    @Test
    void listBooks_unknownSortProperty_returns400() throws Exception {
        mockMvc.perform(get("/apis/v1/books").param("sort", "noSuchField").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Unknown property: noSuchField"));
    }

    @Test
    void corsPreflight_fromAllowedFrontendOrigin_isAllowed() throws Exception {
        mockMvc.perform(options("/apis/v1/books")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    void corsPreflight_fromUnknownOrigin_isRejected() throws Exception {
        mockMvc.perform(options("/apis/v1/books")
                        .header("Origin", "http://evil.example")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
