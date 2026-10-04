package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.BookCreateRequest;
import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookService")
class BookServiceTest {

    private static final String ISBN = "978-0134685991";
    private static final String TITLE = "Effective Java";
    private static final String AUTHOR = "Joshua Bloch";
    private static final String SAME_ISBN_MESSAGE = "Title and Author should be same for same ISBN";

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private BookService bookService;

    @Captor
    private ArgumentCaptor<Book> bookCaptor;

    private static Book book(final String isbn, final String title, final String author) {
        final Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthor(author);
        return book;
    }

    private static BookCreateRequest request(final String isbn, final String title, final String author) {
        final BookCreateRequest request = new BookCreateRequest();
        ReflectionTestUtils.setField(request, "isbn", isbn);
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "author", author);
        return request;
    }

    @Nested
    @DisplayName("registerBook")
    class RegisterBook {

        private BookCreateRequest request;
        private Book mappedBook;

        @BeforeEach
        void setUp() {
            request = request(ISBN, TITLE, AUTHOR);
            mappedBook = book(ISBN, TITLE, AUTHOR);
        }

        @Test
        @DisplayName("new ISBN: saves the book and returns the saved entity")
        void registerBook_newIsbn_savesAndReturnsBook() {
            final Book savedBook = book(ISBN, TITLE, AUTHOR);
            savedBook.setId("generated-id");
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.empty());
            when(bookRepository.save(mappedBook)).thenReturn(savedBook);

            final Book result = bookService.registerBook(request);

            assertThat(result).isSameAs(savedBook);
            assertThat(result.getId()).isEqualTo("generated-id");
        }

        @Test
        @DisplayName("new ISBN: looks up the ISBN exactly once, then saves")
        void registerBook_newIsbn_looksUpIsbnOnceThenSaves() {
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.empty());
            when(bookRepository.save(mappedBook)).thenReturn(mappedBook);

            bookService.registerBook(request);

            verify(bookRepository).findFirstByIsbn(ISBN);
            verify(bookRepository).save(same(mappedBook));
            verifyNoMoreInteractions(bookRepository);
        }

        @Test
        @DisplayName("maps the request to a Book with ModelMapper exactly once")
        void registerBook_anyRequest_usesModelMapperOutput() {
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.empty());
            when(bookRepository.save(mappedBook)).thenReturn(mappedBook);

            bookService.registerBook(request);

            verify(modelMapper).map(same(request), same(Book.class));
            verifyNoMoreInteractions(modelMapper);
            verify(bookRepository).save(same(mappedBook));
        }

        @Test
        @DisplayName("returns exactly what the repository's save returns, not the mapped input")
        void registerBook_saveReturnsDifferentInstance_returnsSavedInstance() {
            final Book persisted = book(ISBN, TITLE, AUTHOR);
            persisted.setId("db-id");
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.empty());
            when(bookRepository.save(mappedBook)).thenReturn(persisted);

            final Book result = bookService.registerBook(request);

            assertThat(result).isSameAs(persisted).isNotSameAs(mappedBook);
        }

        @Test
        @DisplayName("saved Book carries the mapped isbn, title and author")
        void registerBook_newIsbn_savedBookHasMappedFields() {
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.empty());
            when(bookRepository.save(any(Book.class))).thenReturn(mappedBook);

            bookService.registerBook(request);

            verify(bookRepository).save(bookCaptor.capture());
            final Book saved = bookCaptor.getValue();
            assertThat(saved.getIsbn()).isEqualTo(ISBN);
            assertThat(saved.getTitle()).isEqualTo(TITLE);
            assertThat(saved.getAuthor()).isEqualTo(AUTHOR);
        }

        @Test
        @DisplayName("existing ISBN with same title and author: saves another copy")
        void registerBook_sameIsbnSameTitleAndAuthor_savesBook() {
            final Book existing = book(ISBN, TITLE, AUTHOR);
            existing.setId("existing-id");
            final Book savedCopy = book(ISBN, TITLE, AUTHOR);
            savedCopy.setId("new-copy-id");
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.of(existing));
            when(bookRepository.save(mappedBook)).thenReturn(savedCopy);

            final Book result = bookService.registerBook(request);

            assertThat(result).isSameAs(savedCopy);
            verify(bookRepository).findFirstByIsbn(ISBN);
            verify(bookRepository).save(same(mappedBook));
        }

        @Test
        @DisplayName("existing ISBN with different title: throws IllegalStateException and does not save")
        void registerBook_sameIsbnDifferentTitle_throwsIllegalStateException() {
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.of(book(ISBN, "Clean Code", AUTHOR)));

            assertThatThrownBy(() -> bookService.registerBook(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SAME_ISBN_MESSAGE);

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("existing ISBN with different author: throws IllegalStateException and does not save")
        void registerBook_sameIsbnDifferentAuthor_throwsIllegalStateException() {
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.of(book(ISBN, TITLE, "Robert C. Martin")));

            assertThatThrownBy(() -> bookService.registerBook(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SAME_ISBN_MESSAGE);

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("existing ISBN with different title and author: throws IllegalStateException and does not save")
        void registerBook_sameIsbnDifferentTitleAndAuthor_throwsIllegalStateException() {
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.of(book(ISBN, "Clean Code", "Robert C. Martin")));

            assertThatThrownBy(() -> bookService.registerBook(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SAME_ISBN_MESSAGE);

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("title comparison is case-sensitive (documents current behaviour)")
        void registerBook_sameIsbnTitleDiffersOnlyByCase_throwsIllegalStateException() {
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.of(book(ISBN, TITLE.toUpperCase(), AUTHOR)));

            assertThatThrownBy(() -> bookService.registerBook(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SAME_ISBN_MESSAGE);

            verify(bookRepository, never()).save(any(Book.class));
        }

        @Test
        @DisplayName("repository exception from save propagates unchanged")
        void registerBook_saveThrowsDataIntegrityViolation_propagatesException() {
            final DataIntegrityViolationException failure = new DataIntegrityViolationException("constraint violated");
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.empty());
            when(bookRepository.save(mappedBook)).thenThrow(failure);

            assertThatThrownBy(() -> bookService.registerBook(request))
                    .isSameAs(failure);
        }

        @Test
        @DisplayName("existing book with null title: treated as a mismatch, throws IllegalStateException")
        void registerBook_existingBookHasNullTitle_throwsIllegalStateException() {
            when(modelMapper.map(request, Book.class)).thenReturn(mappedBook);
            when(bookRepository.findFirstByIsbn(ISBN)).thenReturn(Optional.of(book(ISBN, null, AUTHOR)));

            assertThatThrownBy(() -> bookService.registerBook(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage(SAME_ISBN_MESSAGE);

            verify(bookRepository, never()).save(any(Book.class));
        }
    }

    @Nested
    @DisplayName("listBooks")
    class ListBooks {

        @Test
        @DisplayName("delegates with the exact same Pageable and returns the repository page unchanged")
        void listBooks_pageRequest_delegatesAndReturnsSamePage() {
            final Pageable pageable = PageRequest.of(0, 1, Sort.by("title"));
            final Page<Book> page = new PageImpl<>(List.of(book(ISBN, TITLE, AUTHOR)), pageable, 3);
            when(bookRepository.findAll(pageable)).thenReturn(page);

            final Page<Book> result = bookService.listBooks(pageable);

            assertThat(result).isSameAs(page);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(3);
            assertThat(result.getTotalPages()).isEqualTo(3);
            verify(bookRepository).findAll(same(pageable));
            verifyNoMoreInteractions(bookRepository);
            verifyNoInteractions(modelMapper);
        }

        @Test
        @DisplayName("empty repository page: returns an empty page")
        void listBooks_noBooks_returnsEmptyPage() {
            final Pageable pageable = PageRequest.of(0, 10);
            final Page<Book> emptyPage = Page.empty(pageable);
            when(bookRepository.findAll(pageable)).thenReturn(emptyPage);

            final Page<Book> result = bookService.listBooks(pageable);

            assertThat(result).isSameAs(emptyPage);
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("unpaged request: passes Pageable.unpaged() through to the repository")
        void listBooks_unpaged_delegatesUnpagedPageable() {
            final Pageable unpaged = Pageable.unpaged();
            final Page<Book> page = new PageImpl<>(List.of(
                    book(ISBN, TITLE, AUTHOR),
                    book("978-0132350884", "Clean Code", "Robert C. Martin")));
            when(bookRepository.findAll(unpaged)).thenReturn(page);

            final Page<Book> result = bookService.listBooks(unpaged);

            assertThat(result).isSameAs(page);
            assertThat(result.getContent()).hasSize(2);
            verify(bookRepository).findAll(same(unpaged));
        }
    }
}
