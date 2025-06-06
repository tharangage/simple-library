package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.model.Book;
import com.ascendion.roshan.simple_library.model.dto.BookCreateRequest;
import com.ascendion.roshan.simple_library.repository.BookRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BookService {

    private final BookRepository bookRepository;

    private final ModelMapper modelMapper;

    public BookService(final BookRepository bookRepository,
                       final ModelMapper modelMapper) {
        this.bookRepository = bookRepository;
        this.modelMapper = modelMapper;
    }

    public Book registerBook(final BookCreateRequest bookCreateRequest) {

        // convert to entity model
        final Book bookNew = modelMapper.map(bookCreateRequest, Book.class);

        final long countBySameIsbn = bookRepository.countByIsbn(bookNew.getIsbn());
        if (countBySameIsbn > 0) {
            log.info("There are {} books with same ISBN {}", countBySameIsbn, bookNew.getIsbn());
            final Book bookwithSameIsbn = bookRepository.findFirstByIsbn(bookNew.getIsbn());
            if (!bookwithSameIsbn.getTitle().equals(bookNew.getTitle())
                    || !bookwithSameIsbn.getAuthor().equals(bookNew.getAuthor())) {
                throw new IllegalArgumentException("Title and Author should be same for same ISBN");
            }
        }

        return bookRepository.save(bookNew);
    }

    public Page<Book> listBooks(final Pageable pageable) {
        return new PageImpl<>(bookRepository.findAll());
    }
}
