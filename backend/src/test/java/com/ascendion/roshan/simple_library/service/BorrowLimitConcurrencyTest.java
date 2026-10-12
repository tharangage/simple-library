package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real database, parallel requests: one borrower trying to take more books than the limit at the same time
 * must end with exactly the limit, because the borrower row is locked while the count is checked (US-04.1).
 */
@SpringBootTest
@ActiveProfiles("h2")
class BorrowLimitConcurrencyTest {

    private static final int PARALLEL_REQUESTS = 8;

    @Autowired
    private BorrowedBookService borrowedBookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    @AfterEach
    void cleanUp() {
        bookRepository.deleteAll();
        borrowerRepository.deleteAll();
    }

    @Test
    void borrowBook_parallelRequestsForDifferentBooks_neverExceedTheLimit() throws Exception {
        final Borrower borrower = new Borrower();
        borrower.setFirstname("Ann");
        final String borrowerId = borrowerRepository.saveAndFlush(borrower).getId();

        final List<String> bookIds = new ArrayList<>();
        for (int i = 0; i < PARALLEL_REQUESTS; i++) {
            final Book book = new Book();
            book.setIsbn("978-0-00-00000" + i + "-0");
            book.setTitle("Book " + i);
            book.setAuthor("Author");
            bookIds.add(bookRepository.saveAndFlush(book).getId());
        }

        final ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_REQUESTS);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<String>> results = new ArrayList<>();
        for (final String bookId : bookIds) {
            final Callable<String> task = () -> {
                start.await();
                try {
                    borrowedBookService.borrowBook(borrowerId, bookId);
                    return "OK";
                } catch (IllegalStateException e) {
                    return e.getMessage();
                }
            };
            results.add(pool.submit(task));
        }
        start.countDown();

        int succeeded = 0;
        int limitReached = 0;
        for (final Future<String> result : results) {
            final String outcome = result.get();
            if ("OK".equals(outcome)) {
                succeeded++;
            } else if ("BORROW_LIMIT_REACHED".equals(outcome)) {
                limitReached++;
            }
        }
        pool.shutdown();

        assertThat(succeeded).isEqualTo(3);
        assertThat(limitReached).isEqualTo(PARALLEL_REQUESTS - 3);
        assertThat(bookRepository.countByBorrowedById(borrowerId)).isEqualTo(3);
    }
}
