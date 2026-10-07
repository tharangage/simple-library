package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.exception.NotFoundException;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Read-check-write runs in one transaction with the book row locked, so two
     * concurrent requests cannot both borrow the same book.
     */
    @Transactional
    public void borrowBook(final String borrowerId,
                           final BorrowBookRequest borrowBookRequest) {

        final Borrower borrower = borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new NotFoundException("Borrower not found"));

        final Book book = bookRepository.findByIdForUpdate(borrowBookRequest.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (book.isBorrowed()) {
            throw new IllegalStateException("Book is already borrowed");
        }

        book.setBorrowed(true);
        book.setBorrowedBy(borrower);
        book.setBorrowedDate(LocalDateTime.now());

        bookRepository.save(book);
    }

    @Transactional
    public void returnBook(final String borrowerId,
                           final String bookId) {
        final Book book = bookRepository.findByIdForUpdate(bookId)
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (!book.isBorrowed()
                || book.getBorrowedBy() == null
                || !book.getBorrowedBy().getId().equals(borrowerId)) {
            throw new IllegalStateException("Book was not borrowed by this borrower");
        }

        book.setBorrowed(false);
        book.setBorrowedBy(null);
        book.setBorrowedDate(null);

        bookRepository.save(book);
    }
}
