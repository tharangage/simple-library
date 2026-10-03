package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.exception.NotFoundException;
import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class BookBorrowerService {
    private final BookRepository bookRepository;
    private final BorrowerRepository borrowerRepository;

    public BookBorrowerService(final BookRepository bookRepository,
                               final BorrowerRepository borrowerRepository) {
        this.bookRepository = bookRepository;
        this.borrowerRepository = borrowerRepository;
    }

    public void borrowBook(final String borrowerId,
                           final BorrowBookRequest borrowBookRequest) {

        // validate borrower
        final Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new NotFoundException("Borrower not found"));

        // validate book
        final Book book = bookRepository.findById(borrowBookRequest.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (book.isBorrowed()) {
            throw new IllegalStateException("Book is already borrowed");
        }

        // update book
        book.setBorrowed(true);
        book.setBorrowedBy(borrower);
        book.setBorrowedDate(LocalDateTime.now());

        bookRepository.save(book);
    }

    public void returnBook(final String borrowerId,
                           final String bookId) {
        // validate book
        final Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (!book.isBorrowed()
                || book.getBorrowedBy() == null
                || !book.getBorrowedBy().getId().equals(borrowerId)) {
            throw new IllegalStateException("Book was not borrowed by this borrower");
        }

        // update book
        book.setBorrowed(false);
        book.setBorrowedBy(null);
        book.setBorrowedDate(null);

        bookRepository.save(book);
    }
}
