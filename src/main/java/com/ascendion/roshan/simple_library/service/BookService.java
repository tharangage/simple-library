package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.repository.BookRepository;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(final BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }
}
