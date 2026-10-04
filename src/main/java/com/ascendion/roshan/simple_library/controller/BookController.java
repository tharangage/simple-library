package com.ascendion.roshan.simple_library.controller;

import com.ascendion.roshan.simple_library.dto.BookCreateRequest;
import com.ascendion.roshan.simple_library.dto.BookResponse;
import com.ascendion.roshan.simple_library.service.BookService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apis/v1/books")
public class BookController {

    private final BookService bookService;

    public BookController(final BookService bookService) {
        this.bookService = bookService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse registerBook(@Valid @RequestBody final BookCreateRequest bookCreateRequest) {
        return BookResponse.from(bookService.registerBook(bookCreateRequest));
    }

    @GetMapping
    public Page<BookResponse> listBooks(@ParameterObject @PageableDefault(size = 20) final Pageable pageable) {
        return bookService.listBooks(pageable).map(BookResponse::from);
    }

}
