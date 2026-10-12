package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.config.SecurityConfig;
import com.ascendion.roshan.simple_library.dto.BorrowedBookResponse;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.exception.NotFoundException;
import com.ascendion.roshan.simple_library.service.BorrowedBookService;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for borrow/return. Also exercises the error mapping in
 * {@code CustomResponseEntityExceptionHandler}.
 */
@WebMvcTest(BorrowedBookController.class)
@Import(SecurityConfig.class)
class BorrowedBookControllerTest {

    private static final String BORROWER_ID = UUID.randomUUID().toString();
    private static final String BOOK_ID = UUID.randomUUID().toString();
    private static final String LIST_URL = "/apis/v1/borrowed-books";
    private static final String URL = "/apis/v1/borrowed-books/{bookId}/borrowers/{borrowerId}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BorrowedBookService borrowedBookService;

    @MockitoBean
    private BorrowerProvisioningService provisioningService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor asUser() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor asAdmin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @BeforeEach
    void provisionedBorrower() {
        final Borrower borrower = new Borrower();
        borrower.setId(BORROWER_ID);
        when(provisioningService.getOrCreate(any(Jwt.class))).thenReturn(borrower);
    }

    @Test
    void borrowBook_noToken_returns401() throws Exception {
        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(borrowedBookService);
    }

    @Test
    void borrowBook_userForAnotherBorrower_returns403() throws Exception {
        mockMvc.perform(post(URL, BOOK_ID, UUID.randomUUID().toString()).with(asUser()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(borrowedBookService);
    }

    @Test
    void borrowBook_userAlsoAdmin_canBorrowForAnotherBorrower() throws Exception {
        final String otherBorrowerId = UUID.randomUUID().toString();

        mockMvc.perform(post(URL, BOOK_ID, otherBorrowerId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(borrowedBookService).borrowBook(otherBorrowerId, BOOK_ID);
    }

    @Test
    void borrowBook_limitReached_returns409WithCode() throws Exception {
        doThrow(new IllegalStateException("BORROW_LIMIT_REACHED"))
                .when(borrowedBookService).borrowBook(BORROWER_ID, BOOK_ID);

        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID).with(asUser()))
                .andExpect(status().isConflict())
                .andExpect(content().string("BORROW_LIMIT_REACHED"));
    }

    @Test
    void borrowBook_adminOnly_borrowsForAnyBorrowerWithoutProvisioning() throws Exception {
        final String otherBorrowerId = UUID.randomUUID().toString();

        mockMvc.perform(post(URL, BOOK_ID, otherBorrowerId).with(asAdmin()))
                .andExpect(status().isOk());

        verify(borrowedBookService).borrowBook(otherBorrowerId, BOOK_ID);
        verifyNoInteractions(provisioningService);
    }

    @Test
    void borrowBook_noRole_returns403() throws Exception {
        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID).with(jwt()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(borrowedBookService, provisioningService);
    }

    @Test
    void returnBook_user_returns403() throws Exception {
        mockMvc.perform(delete(URL, BOOK_ID, BORROWER_ID).with(asUser()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(borrowedBookService, provisioningService);
    }

    @Test
    void returnBook_adminWhoIsAlsoUser_doesNotProvision() throws Exception {
        // Borrow and return share one path, so the provisioning interceptor must look at the HTTP method.
        mockMvc.perform(delete(URL, BOOK_ID, BORROWER_ID)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verifyNoInteractions(provisioningService);
    }

    @Test
    void returnBook_adminForAnyBorrower_doesNotProvision() throws Exception {
        mockMvc.perform(delete(URL, BOOK_ID, BORROWER_ID).with(asAdmin()))
                .andExpect(status().isOk());

        verifyNoInteractions(provisioningService);
    }

    @Test
    void borrowBook_validRequest_returns200AndCallsService() throws Exception {
        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID).with(asUser()))
                .andExpect(status().isOk());

        verify(borrowedBookService).borrowBook(BORROWER_ID, BOOK_ID);
    }

    @Test
    void borrowBook_serviceThrowsNotFound_returns404WithMessage() throws Exception {
        doThrow(new NotFoundException("Borrower not found"))
                .when(borrowedBookService).borrowBook(BORROWER_ID, BOOK_ID);

        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID).with(asUser()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Borrower not found"));
    }

    @Test
    void borrowBook_alreadyBorrowed_returns409WithMessage() throws Exception {
        doThrow(new IllegalStateException("BOOK_NOT_AVAILABLE"))
                .when(borrowedBookService).borrowBook(BORROWER_ID, BOOK_ID);

        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID).with(asUser()))
                .andExpect(status().isConflict())
                .andExpect(content().string("BOOK_NOT_AVAILABLE"));
    }

    @Test
    void borrowBook_rowLockedByAnotherRequest_returns409() throws Exception {
        doThrow(new CannotAcquireLockException("lock timeout"))
                .when(borrowedBookService).borrowBook(BORROWER_ID, BOOK_ID);

        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID).with(asUser()))
                .andExpect(status().isConflict())
                .andExpect(content().string("Resource is being updated by another request, please retry"));
    }

    @Test
    void borrowBook_dataIntegrityViolation_returns409GenericMessage() throws Exception {
        doThrow(new DataIntegrityViolationException("constraint xyz"))
                .when(borrowedBookService).borrowBook(BORROWER_ID, BOOK_ID);

        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID).with(asUser()))
                .andExpect(status().isConflict())
                .andExpect(content().string("Data integrity violation"));
    }

    @Test
    void borrowBook_unexpectedRuntimeException_returns500WithoutDetails() throws Exception {
        doThrow(new IllegalArgumentException("internal detail that must not leak"))
                .when(borrowedBookService).borrowBook(BORROWER_ID, BOOK_ID);

        mockMvc.perform(post(URL, BOOK_ID, BORROWER_ID).with(asUser()))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Internal server error"));
    }

    @Test
    void returnBook_noToken_returns401() throws Exception {
        mockMvc.perform(delete(URL, BOOK_ID, BORROWER_ID))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(borrowedBookService);
    }

    @Test
    void returnBook_validRequest_returns200AndCallsService() throws Exception {
        mockMvc.perform(delete(URL, BOOK_ID, BORROWER_ID).with(asAdmin()))
                .andExpect(status().isOk());

        verify(borrowedBookService).returnBook(BORROWER_ID, BOOK_ID);
    }

    @Test
    void returnBook_notBorrowedByThisBorrower_returns409() throws Exception {
        doThrow(new IllegalStateException("Book was not borrowed by this borrower"))
                .when(borrowedBookService).returnBook(BORROWER_ID, BOOK_ID);

        mockMvc.perform(delete(URL, BOOK_ID, BORROWER_ID).with(asAdmin()))
                .andExpect(status().isConflict())
                .andExpect(content().string("Book was not borrowed by this borrower"));
    }

    @Test
    void listBorrowedBooks_noToken_returns401() throws Exception {
        mockMvc.perform(get(LIST_URL)).andExpect(status().isUnauthorized());

        verifyNoInteractions(borrowedBookService);
    }

    @Test
    void listBorrowedBooks_user_returns403() throws Exception {
        mockMvc.perform(get(LIST_URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(borrowedBookService);
    }

    @Test
    void listBorrowedBooks_admin_returnsPageOfBorrowedBooks() throws Exception {
        final BorrowedBookResponse borrowedBook = new BorrowedBookResponse("book-1", "Clean Code", "Robert C. Martin", "978-0-13-235088-4",
                LocalDateTime.of(2026, 10, 1, 9, 30), "b-1", "Ann", "Lee", "ann@example.com");
        when(borrowedBookService.listBorrowedBooks(eq("clean"), any()))
                .thenReturn(new PageImpl<>(List.of(borrowedBook), PageRequest.of(0, 5), 1));

        mockMvc.perform(get(LIST_URL).param("q", "clean").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].bookId").value("book-1"))
                .andExpect(jsonPath("$.content[0].borrowerId").value("b-1"))
                .andExpect(jsonPath("$.content[0].borrowerEmail").value("ann@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listBorrowedBooks_withoutParameters_defaultsToFiveNewestFirst() throws Exception {
        when(borrowedBookService.listBorrowedBooks(eq(null), any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(LIST_URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(borrowedBookService).listBorrowedBooks(eq(null), argThat(p -> p.getPageSize() == 5
                && p.getSort().getOrderFor("borrowedDate") != null
                && p.getSort().getOrderFor("borrowedDate").isDescending()));
    }
}
