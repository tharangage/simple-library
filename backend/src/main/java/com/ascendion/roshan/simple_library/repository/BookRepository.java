package com.ascendion.roshan.simple_library.repository;

import com.ascendion.roshan.simple_library.entity.Book;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import com.ascendion.roshan.simple_library.dto.BorrowedBookResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {

    Optional<Book> findFirstByIsbn(String isbn);

    /** Books this borrower currently holds. */
    long countByBorrowedById(String borrowerId);

    /**
     * Loads a book with a pessimistic write lock (SELECT ... FOR UPDATE) so that two
     * concurrent borrow/return requests for the same book are serialised.
     * Must be called inside a transaction. Waits at most 3 seconds for the lock, then fails with a lock
     * exception (answered as 409) instead of waiting forever.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select b from Book b where b.id = :id")
    Optional<Book> findByIdForUpdate(@Param("id") String id);

    /**
     * Books whose title, author or ISBN contain the text. {@code pattern} is a lower-case LIKE pattern
     * (see {@code SearchPattern}). Sort properties are those of {@code Book}, e.g. {@code title}, {@code author}.
     */
    @Query("""
            select b from Book b
            where lower(b.title) like :pattern escape '!'
               or lower(b.author) like :pattern escape '!'
               or lower(b.isbn) like :pattern escape '!'
            """)
    Page<Book> searchBooks(@Param("pattern") String pattern, Pageable pageable);

    /**
     * Books that are out, with their borrower. {@code pattern} is a lower-case LIKE pattern (see BorrowedBookService.listBorrowedBooks).
     * Sort properties are those of {@code Book}, e.g. {@code borrowedDate}, {@code title}.
     */
    @Query(value = """
            select new com.ascendion.roshan.simple_library.dto.BorrowedBookResponse(
                b.id, b.title, b.author, b.isbn, b.borrowedDate,
                br.id, br.firstname, br.lastname, br.email)
            from Book b join b.borrowedBy br
            where b.isBorrowed = true
              and (lower(b.id) like :pattern escape '!'
                or lower(b.isbn) like :pattern escape '!'
                or lower(b.title) like :pattern escape '!'
                or lower(br.email) like :pattern escape '!')
            """,
            countQuery = """
            select count(b) from Book b join b.borrowedBy br
            where b.isBorrowed = true
              and (lower(b.id) like :pattern escape '!'
                or lower(b.isbn) like :pattern escape '!'
                or lower(b.title) like :pattern escape '!'
                or lower(br.email) like :pattern escape '!')
            """)
    Page<BorrowedBookResponse> findBorrowedBooks(@Param("pattern") String pattern, Pageable pageable);
}
