package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.repository.BookRepository;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.springframework.stereotype.Service;

@Service
public class BookBorrowerService {
    private final BookRepository bookRepository;
    private final BorrowerRepository borrowerRepository;

    public BookBorrowerService(final BookRepository bookRepository,
                               final BorrowerRepository borrowerRepository) {
        this.bookRepository = bookRepository;
        this.borrowerRepository = borrowerRepository;
    }
}
