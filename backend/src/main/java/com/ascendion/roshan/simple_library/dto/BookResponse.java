package com.ascendion.roshan.simple_library.dto;

import com.ascendion.roshan.simple_library.entity.Book;

import java.time.LocalDateTime;

/**
 * API representation of a book. Keeps the JPA entity out of the HTTP layer;
 * the JSON fields are the same as the entity used to produce.
 */
public record BookResponse(
        String id,
        String isbn,
        String title,
        String author,
        boolean borrowed,
        LocalDateTime borrowedDate,
        LocalDateTime createdDate,
        LocalDateTime lastModifiedDate) {

    public static BookResponse from(final Book book) {
        return new BookResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getAuthor(),
                book.isBorrowed(),
                book.getBorrowedDate(),
                book.getCreatedDate(),
                book.getLastModifiedDate());
    }
}
