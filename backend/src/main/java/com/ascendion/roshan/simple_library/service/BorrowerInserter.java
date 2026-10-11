package com.ascendion.roshan.simple_library.service;

import com.ascendion.roshan.simple_library.entity.Borrower;
import com.ascendion.roshan.simple_library.repository.BorrowerRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inserts a Borrower in its own transaction, so a duplicate-key failure rolls back only this insert
 * and the caller can re-read the row that won the race. A separate bean so the proxy applies.
 */
@Component
public class BorrowerInserter {

    private final BorrowerRepository borrowerRepository;

    public BorrowerInserter(final BorrowerRepository borrowerRepository) {
        this.borrowerRepository = borrowerRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Borrower insert(final Borrower borrower) {
        return borrowerRepository.saveAndFlush(borrower);
    }
}
