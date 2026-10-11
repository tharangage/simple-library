package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.config.LibraryProperties;
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
    /** 409 body: the book is borrowed already. */
    public static final String BOOK_NOT_AVAILABLE = "BOOK_NOT_AVAILABLE";
    /** 409 body: the borrower already holds the maximum number of items. */
    public static final String BORROW_LIMIT_REACHED = "BORROW_LIMIT_REACHED";

    private final BookRepository bookRepository;
    private final BorrowerRepository borrowerRepository;
    private final LibraryProperties libraryProperties;

    public BookBorrowerService(final BookRepository bookRepository,
                               final BorrowerRepository borrowerRepository,
                               final LibraryProperties libraryProperties) {
        this.bookRepository = bookRepository;
        this.borrowerRepository = borrowerRepository;
        this.libraryProperties = libraryProperties;
    }

    /** Books this borrower holds right now. */
    public long borrowedCount(final String borrowerId) {
        return bookRepository.countByBorrowedById(borrowerId);
    }

    public int maxItemsPerBorrower() {
        return libraryProperties.maxItemsPerBorrower();
    }

    /**
     * Read-check-write runs in one transaction with the borrower and the book rows locked, so two
     * concurrent requests can neither borrow the same book nor push one person over the item limit.
     * The borrower is locked first, then the book (return only locks the book, so there is no deadlock).
     */
    @Transactional
    public void borrowBook(final String borrowerId,
                           final BorrowBookRequest borrowBookRequest) {

        final Borrower borrower = borrowerRepository.findByIdForUpdate(borrowerId)
                .orElseThrow(() -> new NotFoundException("Borrower not found"));

        final Book book = bookRepository.findByIdForUpdate(borrowBookRequest.getBookId())
                .orElseThrow(() -> new NotFoundException("Book not found"));

        if (book.isBorrowed()) {
            throw new IllegalStateException(BOOK_NOT_AVAILABLE);
        }
        if (bookRepository.countByBorrowedById(borrowerId) >= libraryProperties.maxItemsPerBorrower()) {
            throw new IllegalStateException(BORROW_LIMIT_REACHED);
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
