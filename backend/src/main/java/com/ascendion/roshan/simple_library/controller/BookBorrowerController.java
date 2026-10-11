package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import com.ascendion.roshan.simple_library.service.BorrowerProvisioningService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

    @PostMapping
    public void borrowBook(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable("borrower-id") final String borrowerId,
                           @Valid @RequestBody final BorrowBookRequest borrowBookRequest) {
        provisioningService.getOrCreate(jwt);   // first sign-in: make sure the caller has a Borrower (US-02.4)
        bookBorrowerService.borrowBook(borrowerId, borrowBookRequest);
    }

    @DeleteMapping("/{book-id}")
    public void returnBook(@AuthenticationPrincipal final Jwt jwt,
                           @PathVariable("borrower-id") final String borrowerId,
                           @PathVariable("book-id") final String bookId) {
        provisioningService.getOrCreate(jwt);
        bookBorrowerService.returnBook(borrowerId, bookId);
    }
}
