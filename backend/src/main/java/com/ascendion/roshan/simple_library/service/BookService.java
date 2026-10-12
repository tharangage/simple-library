package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.dto.BookCreateRequest;
import com.ascendion.roshan.simple_library.entity.Book;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final ModelMapper modelMapper;

    public BookService(final BookRepository bookRepository,
                       final ModelMapper modelMapper) {
        this.bookRepository = bookRepository;
        this.modelMapper = modelMapper;
    }

    public Book registerBook(final BookCreateRequest bookCreateRequest) {
        final Book bookNew = modelMapper.map(bookCreateRequest, Book.class);
        validateIsbnConsistency(bookNew);
        return bookRepository.save(bookNew);
    }

    /**
     * One page of books. A search text narrows the list to books whose title, author or ISBN contain it
     * (case-insensitive); without one, every book is listed.
     */
    public Page<Book> listBooks(final String query, final Pageable pageable) {
        if (SearchPattern.normalize(query).isEmpty()) {
            return bookRepository.findAll(pageable);
        }
        return bookRepository.searchBooks(SearchPattern.contains(query), pageable);
    }

    /**
     * ISBN is the edition identity. Multiple copies are allowed, but all copies
     * for the same ISBN must still describe the same title and author.
     */
    private void validateIsbnConsistency(final Book bookNew) {
        bookRepository.findFirstByIsbn(bookNew.getIsbn())
                .filter(existing -> !Objects.equals(existing.getTitle(), bookNew.getTitle())
                        || !Objects.equals(existing.getAuthor(), bookNew.getAuthor()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Title and Author should be same for same ISBN");
                });
    }
}
