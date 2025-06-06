package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("apis/v1/borrowers/{borrower-id}/books")
public class BookBorrowerController {

    private final BookBorrowerService bookBorrowerService;

    public BookBorrowerController(final BookBorrowerService bookBorrowerService) {
        this.bookBorrowerService = bookBorrowerService;
    }

    @PostMapping
    public void borrowBook(@PathVariable("borrower-id") String borrowerId,
                           @Valid @RequestBody BorrowBookRequest borrowBookRequest) {
        bookBorrowerService.borrowBook(borrowerId, borrowBookRequest);
    }

    @DeleteMapping("/{book-id}")
    public void returnBook(@PathVariable("borrower-id") String borrowerId,
                           @PathVariable("book-id") String bookId) {
        bookBorrowerService.returnBook(borrowerId, bookId);
    }
}
