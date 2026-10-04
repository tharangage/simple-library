package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.config.SecurityConfig;
import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.exception.NotFoundException;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for borrow/return. Also exercises the error mapping in
 * {@code CustomResponseEntityExceptionHandler}.
 */
@WebMvcTest(BookBorrowerController.class)
@Import(SecurityConfig.class)
class BookBorrowerControllerTest {

    private static final String BORROWER_ID = UUID.randomUUID().toString();
    private static final String BOOK_ID = UUID.randomUUID().toString();
    private static final String BORROW_URL = "/apis/v1/borrowers/{borrowerId}/books";
    private static final String RETURN_URL = "/apis/v1/borrowers/{borrowerId}/books/{bookId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookBorrowerService bookBorrowerService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static String borrowBody(final String bookId) {
        return "{\"bookId\":\"" + bookId + "\"}";
    }

    @Test
    void borrowBook_noToken_returns401() throws Exception {
        mockMvc.perform(post(BORROW_URL, BORROWER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(BOOK_ID)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookBorrowerService);
    }

    @Test
    void borrowBook_validRequest_returns200AndCallsService() throws Exception {
        mockMvc.perform(post(BORROW_URL, BORROWER_ID).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(BOOK_ID)))
                .andExpect(status().isOk());

        final ArgumentCaptor<BorrowBookRequest> captor = ArgumentCaptor.forClass(BorrowBookRequest.class);
        verify(bookBorrowerService).borrowBook(eq(BORROWER_ID), captor.capture());
        assertThat(captor.getValue().getBookId()).isEqualTo(BOOK_ID);
    }

    @Test
    void borrowBook_bookIdNotUuid_returns400WithFieldMap() throws Exception {
        mockMvc.perform(post(BORROW_URL, BORROWER_ID).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody("not-a-uuid")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.bookId").value("BookId must be a UUID"));

        verifyNoInteractions(bookBorrowerService);
    }

    @Test
    void borrowBook_blankBookId_returns400WithFieldMap() throws Exception {
        mockMvc.perform(post(BORROW_URL, BORROWER_ID).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.bookId").exists());
    }

    @Test
    void borrowBook_serviceThrowsNotFound_returns404WithMessage() throws Exception {
        doThrow(new NotFoundException("Borrower not found"))
                .when(bookBorrowerService).borrowBook(eq(BORROWER_ID), any(BorrowBookRequest.class));

        mockMvc.perform(post(BORROW_URL, BORROWER_ID).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(BOOK_ID)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Borrower not found"));
    }

    @Test
    void borrowBook_alreadyBorrowed_returns409WithMessage() throws Exception {
        doThrow(new IllegalStateException("Book is already borrowed"))
                .when(bookBorrowerService).borrowBook(eq(BORROWER_ID), any(BorrowBookRequest.class));

        mockMvc.perform(post(BORROW_URL, BORROWER_ID).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(BOOK_ID)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Book is already borrowed"));
    }

    @Test
    void borrowBook_rowLockedByAnotherRequest_returns409() throws Exception {
        doThrow(new CannotAcquireLockException("lock timeout"))
                .when(bookBorrowerService).borrowBook(eq(BORROWER_ID), any(BorrowBookRequest.class));

        mockMvc.perform(post(BORROW_URL, BORROWER_ID).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(BOOK_ID)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Resource is being updated by another request, please retry"));
    }

    @Test
    void borrowBook_dataIntegrityViolation_returns409GenericMessage() throws Exception {
        doThrow(new DataIntegrityViolationException("constraint xyz"))
                .when(bookBorrowerService).borrowBook(eq(BORROWER_ID), any(BorrowBookRequest.class));

        mockMvc.perform(post(BORROW_URL, BORROWER_ID).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(BOOK_ID)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Data integrity violation"));
    }

    @Test
    void borrowBook_unexpectedRuntimeException_returns500WithoutDetails() throws Exception {
        doThrow(new IllegalArgumentException("internal detail that must not leak"))
                .when(bookBorrowerService).borrowBook(eq(BORROWER_ID), any(BorrowBookRequest.class));

        mockMvc.perform(post(BORROW_URL, BORROWER_ID).with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(borrowBody(BOOK_ID)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal server error"));
    }

    @Test
    void returnBook_noToken_returns401() throws Exception {
        mockMvc.perform(delete(RETURN_URL, BORROWER_ID, BOOK_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bookBorrowerService);
    }

    @Test
    void returnBook_validRequest_returns200AndCallsService() throws Exception {
        mockMvc.perform(delete(RETURN_URL, BORROWER_ID, BOOK_ID).with(jwt()))
                .andExpect(status().isOk());

        verify(bookBorrowerService).returnBook(BORROWER_ID, BOOK_ID);
    }

    @Test
    void returnBook_notBorrowedByThisBorrower_returns409() throws Exception {
        doThrow(new IllegalStateException("Book was not borrowed by this borrower"))
                .when(bookBorrowerService).returnBook(BORROWER_ID, BOOK_ID);

        mockMvc.perform(delete(RETURN_URL, BORROWER_ID, BOOK_ID).with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(content().string("Book was not borrowed by this borrower"));
    }
}
