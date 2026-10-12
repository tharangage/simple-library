package com.ascendion.roshan.simple_library.repository;

import com.ascendion.roshan.simple_library.dto.BorrowedBookResponse;
import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.entity.Borrower;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    private Book copy(final String isbn, final String title, final String author) {
        final Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        return bookRepository.saveAndFlush(book);
    }

    @Test
    void searchBooks_matchesTitleAuthorAndIsbnCaseInsensitively() {
        copy("978-0-13-235088-4", "Clean Code", "Robert C. Martin");
        copy("978-0-13-468599-1", "Effective Java", "Joshua Bloch");

        assertThat(bookRepository.searchBooks("%clean%", FIRST_PAGE).getContent())
                .extracting(Book::getTitle).containsExactly("Clean Code");
        assertThat(bookRepository.searchBooks("%bloch%", FIRST_PAGE).getContent())
                .extracting(Book::getTitle).containsExactly("Effective Java");
        assertThat(bookRepository.searchBooks("%468599%", FIRST_PAGE).getContent())
                .extracting(Book::getTitle).containsExactly("Effective Java");
        assertThat(bookRepository.searchBooks("%nothing%", FIRST_PAGE).getTotalElements()).isZero();
    }

    @Test
    void searchBooks_returnsBorrowedAndAvailableCopies_andPages() {
        final Borrower ann = borrower("ann@example.com");
        book("Dune", ann);
        book("Dune Messiah", null);
        book("Dune Children", null);

        final var page = bookRepository.searchBooks("%dune%", PageRequest.of(1, 2, Sort.by("title")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(Book::getTitle).containsExactly("Dune Messiah");
    }

    @Test
    void searchBooks_escapedWildcard_isSearchedAsText() {
        copy("978-0-00-000001-0", "100% Pure", "A");
        copy("978-0-00-000002-0", "1000 Ways", "B");

        assertThat(bookRepository.searchBooks("%100!%%", FIRST_PAGE).getContent())
                .extracting(Book::getTitle).containsExactly("100% Pure");
    }

    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 10, Sort.by("title"));

    @Test
    void findBorrowedBooks_returnsOnlyBorrowedBooksWithBorrowerDetails() {
        final Borrower ann = borrower("ann@example.com");
        book("Borrowed", ann);
        book("On the shelf", null);

        final var page = bookRepository.findBorrowedBooks("%", FIRST_PAGE);

        assertThat(page.getTotalElements()).isEqualTo(1);
        final BorrowedBookResponse borrowedBook = page.getContent().get(0);
        assertThat(borrowedBook.title()).isEqualTo("Borrowed");
        assertThat(borrowedBook.borrowerId()).isEqualTo(ann.getId());
        assertThat(borrowedBook.borrowerFirstname()).isEqualTo("Ann");
        assertThat(borrowedBook.borrowerEmail()).isEqualTo("ann@example.com");
    }

    @Test
    void findBorrowedBooks_matchesTitleAndBorrowerEmailCaseInsensitively() {
        final Borrower ann = borrower("ann@example.com");
        final Borrower bob = borrower("bob@example.com");
        book("Clean Code", ann);
        book("Effective Java", bob);

        assertThat(bookRepository.findBorrowedBooks("%clean%", FIRST_PAGE).getContent())
                .extracting(BorrowedBookResponse::title).containsExactly("Clean Code");
        assertThat(bookRepository.findBorrowedBooks("%bob@example%", FIRST_PAGE).getContent())
                .extracting(BorrowedBookResponse::title).containsExactly("Effective Java");
    }

    @Test
    void findBorrowedBooks_matchesBookIdAndIsbn() {
        final Borrower ann = borrower("ann@example.com");
        book("Clean Code", ann);
        final String bookId = bookRepository.findAll().get(0).getId();

        assertThat(bookRepository.findBorrowedBooks("%" + bookId + "%", FIRST_PAGE).getTotalElements()).isEqualTo(1);
        assertThat(bookRepository.findBorrowedBooks("%978-0-00%", FIRST_PAGE).getTotalElements()).isEqualTo(1);
        assertThat(bookRepository.findBorrowedBooks("%nothing%", FIRST_PAGE).getTotalElements()).isZero();
    }

    @Test
    void findBorrowedBooks_pagesTheResult() {
        final Borrower ann = borrower("ann@example.com");
        book("A", ann);
        book("B", ann);
        book("C", ann);

        final var page = bookRepository.findBorrowedBooks("%", PageRequest.of(1, 2, Sort.by("title")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(BorrowedBookResponse::title).containsExactly("C");
    }
}
