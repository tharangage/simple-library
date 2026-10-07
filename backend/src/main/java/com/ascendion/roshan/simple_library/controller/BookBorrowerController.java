package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import jakarta.validation.Valid;
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

    public BookBorrowerController(final BookBorrowerService bookBorrowerService) {
        this.bookBorrowerService = bookBorrowerService;
    }

    @PostMapping
    public void borrowBook(@PathVariable("borrower-id") final String borrowerId,
                           @Valid @RequestBody final BorrowBookRequest borrowBookRequest) {
        bookBorrowerService.borrowBook(borrowerId, borrowBookRequest);
    }

    @DeleteMapping("/{book-id}")
    public void returnBook(@PathVariable("borrower-id") final String borrowerId,
                           @PathVariable("book-id") final String bookId) {
        bookBorrowerService.returnBook(borrowerId, bookId);
    }
}
