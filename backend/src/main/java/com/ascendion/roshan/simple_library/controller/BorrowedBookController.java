package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowedBookResponse;
import com.ascendion.roshan.simple_library.security.CurrentUser;
import com.ascendion.roshan.simple_library.security.CurrentUserHolder;
import com.ascendion.roshan.simple_library.service.BorrowedBookService;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A book that is out is a resource: borrowing creates it, returning deletes it. */
@RestController
@RequestMapping("/apis/v1/borrowed-books")
public class BorrowedBookController {

    private final BorrowedBookService borrowedBookService;
    private final BorrowerProvisioningService provisioningService;

    public BorrowedBookController(final BorrowedBookService borrowedBookService,
                                  final BorrowerProvisioningService provisioningService) {
        this.borrowedBookService = borrowedBookService;
        this.provisioningService = provisioningService;
    }

    /** Admin only, enforced in {@code SecurityConfig}. Returning a borrowed book uses the DELETE endpoint below. */
    @GetMapping
    public Page<BorrowedBookResponse> listBorrowedBooks(
            @RequestParam(name = "q", required = false) final String query,
            @ParameterObject @PageableDefault(size = 5, sort = "borrowedDate", direction = Sort.Direction.DESC)
            final Pageable pageable) {
        return borrowedBookService.listBorrowedBooks(query, pageable);
    }

    /**
     * An admin borrows on behalf of any borrower at the library desk. Everybody else may borrow only for their
     * own Borrower, which belongs to the token's {@code sub}.
     */
    @PostMapping("/{book-id}/borrowers/{borrower-id}")
    public void borrowBook(@PathVariable("book-id") final String bookId,
                           @PathVariable("borrower-id") final String borrowerId) {
        final CurrentUser caller = CurrentUserHolder.require();
        if (!caller.isAdmin() && !provisioningService.getOrCreate(caller.jwt()).getId().equals(borrowerId)) {
            throw new AccessDeniedException("A user can borrow only for themselves");
        }
        borrowedBookService.borrowBook(borrowerId, bookId);
    }

    /** Admin only, enforced in {@code SecurityConfig}. */
    @DeleteMapping("/{book-id}/borrowers/{borrower-id}")
    public void returnBook(@PathVariable("book-id") final String bookId,
                           @PathVariable("borrower-id") final String borrowerId) {
        borrowedBookService.returnBook(borrowerId, bookId);
    }
}
