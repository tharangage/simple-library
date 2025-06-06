package com.ascendion.roshan.simple_library.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
public class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetBooksApi() throws Exception {
        mockMvc.perform(get("/apis/v1/books"))
                .andExpect(status().isOk());
    }

    @Test
    public void testRegisterBooksApi() throws Exception {
        mockMvc.perform(post("/apis/v1/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                          "isbn": "978-955-675-510-7",
                          "title": "string",
                          "author": "string"
                        }"""))
                .andExpect(status().isCreated());
    }
}
