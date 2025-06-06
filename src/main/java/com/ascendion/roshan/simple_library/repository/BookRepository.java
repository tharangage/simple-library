package com.ascendion.roshan.simple_library.repository;

import com.ascendion.roshan.simple_library.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {
    long countByIsbn(String isbn);

    Book findFirstByIsbn(String isbn);
}
