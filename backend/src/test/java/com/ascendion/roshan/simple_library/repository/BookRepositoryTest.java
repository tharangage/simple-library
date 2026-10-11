package com.ascendion.roshan.simple_library.repository;

import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.entity.Borrower;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BorrowerRepository borrowerRepository;

    private Borrower borrower(final String email) {
        final Borrower borrower = new Borrower();
        borrower.setFirstname("Ann");
        borrower.setEmail(email);
        return borrowerRepository.saveAndFlush(borrower);
    }

    private void book(final String title, final Borrower borrowedBy) {
        final Book book = new Book();
        book.setIsbn("978-0-00-000000-0");
        book.setTitle(title);
        book.setAuthor("Author");
        if (borrowedBy != null) {
            book.setBorrowed(true);
            book.setBorrowedBy(borrowedBy);
        }
        bookRepository.saveAndFlush(book);
    }

    @Test
    void countByBorrowedById_countsOnlyThisBorrowersBooks() {
        final Borrower ann = borrower("ann@example.com");
        final Borrower bob = borrower("bob@example.com");
        book("A", ann);
        book("B", ann);
        book("C", bob);
        book("D", null);

        assertThat(bookRepository.countByBorrowedById(ann.getId())).isEqualTo(2);
        assertThat(bookRepository.countByBorrowedById(bob.getId())).isEqualTo(1);
    }

    @Test
    void countByBorrowedById_borrowerWithoutBooks_returnsZero() {
        final Borrower ann = borrower("ann@example.com");

        assertThat(bookRepository.countByBorrowedById(ann.getId())).isZero();
    }
}
