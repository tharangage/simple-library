package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.security.CurrentUser;
import com.ascendion.roshan.simple_library.security.CurrentUserHolder;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/borrowers/{borrower-id}/books")
public class BookBorrowerController {

    private final BookBorrowerService bookBorrowerService;
    private final BorrowerProvisioningService provisioningService;

    public BookBorrowerController(final BookBorrowerService bookBorrowerService,
                                  final BorrowerProvisioningService provisioningService) {
        this.bookBorrowerService = bookBorrowerService;
        this.provisioningService = provisioningService;
    }

    /**
     * An admin borrows on behalf of any borrower at the library desk. Everybody else may borrow only for their
     * own Borrower, which belongs to the token's {@code sub}.
     */
    @PostMapping
    public void borrowBook(@PathVariable("borrower-id") final String borrowerId,
                           @Valid @RequestBody final BorrowBookRequest borrowBookRequest) {
        final CurrentUser caller = CurrentUserHolder.require();
        if (!caller.isAdmin() && !provisioningService.getOrCreate(caller.jwt()).getId().equals(borrowerId)) {
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
