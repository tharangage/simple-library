package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.model.Book;
import com.ascendion.roshan.simple_library.model.dto.BookCreateRequest;
import com.ascendion.roshan.simple_library.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/apis/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(final BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    public Book registerBook(@RequestBody BookCreateRequest bookCreateRequest) {
        return bookService.registerBook(bookCreateRequest);
    }

    @GetMapping
    public Page<Book> listBooks(@PageableDefault(size = 20) Pageable pageable) {
        return bookService.listBooks(pageable);
    }

}
