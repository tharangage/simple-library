package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.model.BorrowBookRequest;
import com.ascendion.roshan.simple_library.service.BookBorrowerService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("apis/v1/borrowers/{borrower-id}/books")
public class BookBorrowerController {

    private final BookBorrowerService bookBorrowerService;

    public BookBorrowerController(final BookBorrowerService bookBorrowerService) {
        this.bookBorrowerService = bookBorrowerService;
    }

    @PostMapping
    public void borrowBook(@RequestBody BorrowBookRequest borrowBookRequest) {
        //TODO
    }

    @DeleteMapping("/{book-id}")
    public void returnBook(@PathVariable("book-id") String bookId) {
        //TODO
    }
}
