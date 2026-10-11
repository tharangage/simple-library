package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.config.BorrowerProvisioningInterceptor;
import com.ascendion.roshan.simple_library.config.SecurityConfig;
import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/borrowers/{borrower-id}/books")
public class BookBorrowerController {

    private final BookBorrowerService bookBorrowerService;

    public BookBorrowerController(final BookBorrowerService bookBorrowerService) {
        this.bookBorrowerService = bookBorrowerService;
    }

    /**
     * An admin borrows on behalf of any borrower at the library desk. Everybody else may borrow only for their
     * own Borrower, which the interceptor has looked up from the token.
     */
    @PostMapping
    public void borrowBook(final Authentication authentication,
                           @RequestAttribute(name = BorrowerProvisioningInterceptor.CURRENT_BORROWER, required = false)
                           final Borrower currentBorrower,
                           @PathVariable("borrower-id") final String borrowerId,
                           @Valid @RequestBody final BorrowBookRequest borrowBookRequest) {
        final boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> SecurityConfig.ROLE_ADMIN.equals(a.getAuthority()));
        if (!admin && (currentBorrower == null || !currentBorrower.getId().equals(borrowerId))) {
            throw new AccessDeniedException("A user can borrow only for themselves");
        }
        bookBorrowerService.borrowBook(borrowerId, borrowBookRequest);
    }

    /** Admin only, enforced in {@code SecurityConfig}. */
    @DeleteMapping("/{book-id}")
    public void returnBook(@PathVariable("borrower-id") final String borrowerId,
                           @PathVariable("book-id") final String bookId) {
        bookBorrowerService.returnBook(borrowerId, bookId);
    }
}
