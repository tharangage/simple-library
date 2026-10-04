package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.exception.NotFoundException;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookBorrowerServiceTest {

    private static final String BORROWER_ID = UUID.randomUUID().toString();
    private static final String OTHER_BORROWER_ID = UUID.randomUUID().toString();
    private static final String BOOK_ID = UUID.randomUUID().toString();

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @InjectMocks
    private BookBorrowerService bookBorrowerService;

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

    private static BorrowBookRequest request() {
        return BorrowBookRequest.builder().bookId(BOOK_ID).build();
    }

    @Nested
    class BorrowBook {

        @Test
        void borrowBook_unknownBorrower_throwsNotFound() {
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookBorrowerService.borrowBook(BORROWER_ID, request()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Borrower not found");
            verifyNoInteractions(bookRepository);
        }

        @Test
        void borrowBook_unknownBook_throwsNotFound() {
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookBorrowerService.borrowBook(BORROWER_ID, request()))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Book not found");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void borrowBook_alreadyBorrowed_throwsIllegalStateAndDoesNotSave() {
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(bookBorrowedBy(OTHER_BORROWER_ID)));

            assertThatThrownBy(() -> bookBorrowerService.borrowBook(BORROWER_ID, request()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Book is already borrowed");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void borrowBook_availableBook_marksBorrowedAndSaves() {
            final Borrower borrower = borrower(BORROWER_ID);
            final Book book = availableBook();
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));
            final LocalDateTime before = LocalDateTime.now();

            bookBorrowerService.borrowBook(BORROWER_ID, request());

            verify(bookRepository).save(book);
            assertThat(book.isBorrowed()).isTrue();
            assertThat(book.getBorrowedBy()).isSameAs(borrower);
            assertThat(book.getBorrowedDate()).isAfterOrEqualTo(before);
        }

        @Test
        void borrowBook_anyRequest_loadsBookWithLock() {
            when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower(BORROWER_ID)));
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(availableBook()));

            bookBorrowerService.borrowBook(BORROWER_ID, request());

            verify(bookRepository).findByIdForUpdate(BOOK_ID);
            verify(bookRepository, never()).findById(any());
        }
    }

    @Nested
    class ReturnBook {

        @Test
        void returnBook_unknownBook_throwsNotFound() {
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookBorrowerService.returnBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage("Book not found");
        }

        @Test
        void returnBook_bookNotBorrowed_throwsIllegalState() {
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(availableBook()));

            assertThatThrownBy(() -> bookBorrowerService.returnBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Book was not borrowed by this borrower");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void returnBook_borrowedFlagButNoBorrower_throwsIllegalState() {
            final Book book = availableBook();
            book.setBorrowed(true);
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));

            assertThatThrownBy(() -> bookBorrowerService.returnBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Book was not borrowed by this borrower");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void returnBook_borrowedByAnotherBorrower_throwsIllegalState() {
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(bookBorrowedBy(OTHER_BORROWER_ID)));

            assertThatThrownBy(() -> bookBorrowerService.returnBook(BORROWER_ID, BOOK_ID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Book was not borrowed by this borrower");
            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        void returnBook_borrowedByThisBorrower_clearsBorrowStateAndSaves() {
            final Book book = bookBorrowedBy(BORROWER_ID);
            when(bookRepository.findByIdForUpdate(BOOK_ID)).thenReturn(Optional.of(book));

            bookBorrowerService.returnBook(BORROWER_ID, BOOK_ID);

            verify(bookRepository).save(book);
            assertThat(book.isBorrowed()).isFalse();
            assertThat(book.getBorrowedBy()).isNull();
            assertThat(book.getBorrowedDate()).isNull();
        }
    }
}
