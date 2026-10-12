package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.config.LibraryProperties;
import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.exception.NotFoundException;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowedBookServiceTest {

    private static final String BORROWER_ID = UUID.randomUUID().toString();
    private static final String OTHER_BORROWER_ID = UUID.randomUUID().toString();
    private static final String BOOK_ID = UUID.randomUUID().toString();

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    private BorrowedBookService borrowedBookService;

    @BeforeEach
    void setUp() {
        borrowedBookService = new BorrowedBookService(bookRepository, borrowerRepository, new LibraryProperties(3));
    }

    private static Borrower borrower(final String id) {
        final Borrower borrower = new Borrower();
        borrower.setId(id);
        return borrower;
    }

    private static Book availableBook() {
        final Book book = new Book();
        book.setId(BOOK_ID);
        return book;
    }

    private static Book bookBorrowedBy(final String borrowerId) {
        final Book book = availableBook();
        book.setBorrowed(true);
        book.setBorrowedBy(borrower(borrowerId));
        book.setBorrowedDate(LocalDateTime.now().minusDays(1));
        return book;
    }

    @Nested
    class BorrowBook {

        @Test
        void borrowBook_unknownBorrower_throwsNotFound() {
            when(borrowerRepository.findByIdForUpdate(BORROWER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowedBookService.borrowBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Borrower not found");
            verifyNoInteractions(bookRepository);
        }

        @Test
        void borrowBook_unknownBook_throwsNotFound() {
            when(borrowerRepository.findByIdForUpdate(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowedBookService.borrowBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Book not found");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void borrowBook_alreadyBorrowed_throwsIllegalStateAndDoesNotSave() {
            when(borrowerRepository.findByIdForUpdate(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(bookBorrowedBy(OTHER_BORROWER_ID)));

            assertThatThrownBy(() -> borrowedBookService.borrowBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("BOOK_NOT_AVAILABLE");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void borrowBook_borrowerAtLimit_throwsLimitReachedAndDoesNotSave() {
            when(borrowerRepository.findByIdForUpdate(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(availableBook()));
            when(bookRepository.countByBorrowedById(BORROWER_ID)).thenReturn(3L);

            assertThatThrownBy(() -> borrowedBookService.borrowBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("BORROW_LIMIT_REACHED");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void borrowBook_borrowerBelowLimit_borrows() {
            final Book book = availableBook();
            when(borrowerRepository.findByIdForUpdate(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));
            when(bookRepository.countByBorrowedById(BORROWER_ID)).thenReturn(2L);

            borrowedBookService.borrowBook(BORROWER_ID, BOOK_ID);

            assertThat(book.isBorrowed()).isTrue();
        }

        @Test
        void borrowBook_bookAlreadyBorrowedAndBorrowerAtLimit_reportsBookNotAvailable() {
            when(borrowerRepository.findByIdForUpdate(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(bookBorrowedBy(OTHER_BORROWER_ID)));

            assertThatThrownBy(() -> borrowedBookService.borrowBook(BORROWER_ID, BOOK_ID))
                    .hasMessage("BOOK_NOT_AVAILABLE");
        }

        @Test
        void borrowBook_availableBook_marksBorrowedAndSaves() {
            final Borrower borrower = borrower(BORROWER_ID);
            final Book book = availableBook();
            when(borrowerRepository.findByIdForUpdate(BORROWER_ID)).thenReturn(Optional.of(borrower));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));
            final LocalDateTime before = LocalDateTime.now();

            borrowedBookService.borrowBook(BORROWER_ID, BOOK_ID);

            verify(bookRepository).save(book);
            assertThat(book.isBorrowed()).isTrue();
            assertThat(book.getBorrowedBy()).isSameAs(borrower);
            assertThat(book.getBorrowedDate()).isAfterOrEqualTo(before);
        }

        @Test
        void borrowBook_anyRequest_loadsBookWithLock() {
            when(borrowerRepository.findByIdForUpdate(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(availableBook()));

            borrowedBookService.borrowBook(BORROWER_ID, BOOK_ID);

            verify(bookRepository).findByIdForUpdate(BOOK_ID);
            verify(bookRepository, never()).findById(any());
        }
    }

    @Nested
    class Counts {

        @Test
        void borrowedCount_delegatesToRepository() {
            when(bookRepository.countByBorrowedById(BORROWER_ID)).thenReturn(2L);

            assertThat(borrowedBookService.borrowedCount(BORROWER_ID)).isEqualTo(2L);
            assertThat(borrowedBookService.maxItemsPerBorrower()).isEqualTo(3);
        }
    }

    @Nested
    class ReturnBook {

        @Test
        void returnBook_unknownBook_throwsNotFound() {
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> borrowedBookService.returnBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Book not found");
        }

        @Test
        void returnBook_bookNotBorrowed_throwsIllegalState() {
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(availableBook()));

            assertThatThrownBy(() -> borrowedBookService.returnBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Book was not borrowed by this borrower");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void returnBook_borrowedFlagButNoBorrower_throwsIllegalState() {
            final Book book = availableBook();
            book.setBorrowed(true);
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));

            assertThatThrownBy(() -> borrowedBookService.returnBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Book was not borrowed by this borrower");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void returnBook_borrowedByAnotherBorrower_throwsIllegalState() {
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(bookBorrowedBy(OTHER_BORROWER_ID)));

            assertThatThrownBy(() -> borrowedBookService.returnBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Book was not borrowed by this borrower");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void returnBook_borrowedByThisBorrower_clearsBorrowStateAndSaves() {
            final Book book = bookBorrowedBy(BORROWER_ID);
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));

            borrowedBookService.returnBook(BORROWER_ID, BOOK_ID);

            verify(bookRepository).save(book);
            assertThat(book.isBorrowed()).isFalse();
            assertThat(book.getBorrowedBy()).isNull();
            assertThat(book.getBorrowedDate()).isNull();
        }
    }

    @Nested
    class ListBorrowedBooks {

        private final Pageable page = PageRequest.of(0, 5);

        @Test
        void listBorrowedBooks_noQuery_matchesEverything() {
            when(bookRepository.findBorrowedBooks(any(), eq(page))).thenReturn(new PageImpl<>(List.of()));

            assertThat(borrowedBookService.listBorrowedBooks(null, page)).isEmpty();

            verify(bookRepository).findBorrowedBooks("%%", page);
        }

        @Test
        void listBorrowedBooks_query_isTrimmedLowerCasedAndWrapped() {
            when(bookRepository.findBorrowedBooks(any(), eq(page))).thenReturn(new PageImpl<>(List.of()));

            borrowedBookService.listBorrowedBooks("  Clean CODE ", page);

            verify(bookRepository).findBorrowedBooks("%clean code%", page);
        }

        @Test
        void listBorrowedBooks_likeWildcardsInQuery_areEscaped() {
            when(bookRepository.findBorrowedBooks(any(), eq(page))).thenReturn(new PageImpl<>(List.of()));

            borrowedBookService.listBorrowedBooks("50%_!", page);

            verify(bookRepository).findBorrowedBooks("%50!%!_!!%", page);
        }
    }
}
