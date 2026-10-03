package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.BorrowBookRequest;
import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.exception.NotFoundException;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookBorrowerServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BorrowerRepository borrowerRepository;

    @InjectMocks
    private BookBorrowerService bookBorrowerService;

    @Test
    public void borrowWithNonExistingBorrower() {
        final String borrowerId = UUID.randomUUID().toString();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.empty());

        final NotFoundException exception = Assertions.assertThrows(NotFoundException.class, () ->
                bookBorrowerService.borrowBook(
                        borrowerId,
                        BorrowBookRequest.builder().bookId(UUID.randomUUID().toString()).build())
        );
        assertEquals("Borrower not found", exception.getMessage());
    }

    @Test
    public void borrowWithNonExistingBook() {
        final String borrowerId = UUID.randomUUID().toString();
        final String bookId = UUID.randomUUID().toString();

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(mock(Borrower.class)));
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        final NotFoundException exception = Assertions.assertThrows(NotFoundException.class, () ->
                bookBorrowerService.borrowBook(
                        borrowerId,
                        BorrowBookRequest.builder().bookId(bookId).build())
        );
        assertEquals("Book not found", exception.getMessage());
    }

    @Test
    public void borrowAlreadyBorrowedBook() {
        final String borrowerId = UUID.randomUUID().toString();
        final String bookId = UUID.randomUUID().toString();

        final Book book = mock(Book.class);
        when(book.isBorrowed()).thenReturn(true);

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(mock(Borrower.class)));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        final IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class, () ->
                bookBorrowerService.borrowBook(
                        borrowerId,
                        BorrowBookRequest.builder().bookId(bookId).build())
        );
        assertEquals("Book is already borrowed", exception.getMessage());
    }

    @Test
    public void borrowBookSuccessfully() {
        final String borrowerId = UUID.randomUUID().toString();
        final String bookId = UUID.randomUUID().toString();

        final Book book = mock(Book.class);
        when(book.isBorrowed()).thenReturn(false);

        when(borrowerRepository.findById(borrowerId)).thenReturn(Optional.of(mock(Borrower.class)));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

        bookBorrowerService.borrowBook(
                borrowerId,
                BorrowBookRequest.builder().bookId(bookId).build());

        verify(bookRepository, times(1)).save(book);
    }
}
