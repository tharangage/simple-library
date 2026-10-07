package com.ascendion.roshan.simple_library.repository;

import com.ascendion.roshan.simple_library.entity.Book;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {

    Optional<Book> findFirstByIsbn(String isbn);

    /**
     * Loads a book with a pessimistic write lock (SELECT ... FOR UPDATE) so that two
     * concurrent borrow/return requests for the same book are serialised.
     * Must be called inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") String id);
}
